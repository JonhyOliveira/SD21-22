package tp1.impl.servers.common;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import tp1.api.FileDelta;
import tp1.api.FileInfo;
import tp1.api.User;
import tp1.api.service.java.Result;
import util.Token;

import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static tp1.api.service.java.Result.*;
import static tp1.api.service.java.Result.ErrorCode.*;
import static tp1.impl.clients.Clients.FilesClients;
import static tp1.impl.clients.Clients.UsersClients;

public class JavaDirectoryState {

    static final int NUMBER_OF_BACKUPS = 2;

    static final long USER_CACHE_EXPIRATION = 3000;

    final LoadingCache<UserInfo, Result<User>> users = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofMillis(USER_CACHE_EXPIRATION))
            .build(new CacheLoader<>() {
                @Override
                public Result<User> load(UserInfo info) throws Exception {
                    var res = UsersClients.get().getUser(info.userId(), info.password());
                    if (res.error() == ErrorCode.TIMEOUT)
                        return error(BAD_REQUEST);
                    else
                        return res;
                }
            });

    final static Logger Log = Logger.getLogger(JavaDirectoryState.class.getName());

    final Map<String, ExtendedFileInfo> files = new ConcurrentHashMap<>();
    final Map<String, UserFiles> userFiles = new ConcurrentHashMap<>();
    final Map<URI, FileCounts> fileCounts = new ConcurrentHashMap<>();

    public Result<FileDelta> writeFile(String filename, String userId, String password) {

        if (badParam(filename) || badParam(userId))
            return error(BAD_REQUEST);

        var user = getUser(userId, password);
        if (!user.isOK())
            return error(user.error());

        var uf = userFiles.computeIfAbsent(userId, (k) -> new UserFiles());
        synchronized (uf) {
            var fileId = fileId(filename, userId);
            var file = files.get(fileId);
            var info = file != null ? file.info() : new FileInfo();


            var candidates = orderCandidateFileServers(file);

            var newURIs = candidates.stream().map(URI::toString).limit(NUMBER_OF_BACKUPS).collect(Collectors.toList());
            var oldURIs = (file != null ? file.uris() : new LinkedList<URI>()).stream().map(Objects::toString).collect(Collectors.toList());

            var addedURIs = newURIs.stream().filter(uri -> !oldURIs.contains(uri)).collect(Collectors.toSet());
            var removedURIs = oldURIs.stream().filter(uri -> !newURIs.contains(uri)).collect(Collectors.toSet());

            if (addedURIs.size() > 0 || removedURIs.size() > 0)
                return ok(new FileDelta(userId, filename, false, addedURIs, removedURIs, null, null));
            else if (newURIs.size() > 0)
                return ok();
            else
                return error(BAD_REQUEST); // no servers available
        }

    }

    /**
     * Applies a file diff
     * @param delta the file diff
     * @param leader unlocks leader-only actions
     * @param data data associated with this diff
     * @return A FileDelta specifying how to undo actions that were unable to be executed. E.g. while waiting for the
     * delta to be applied something changed.
     */
    public synchronized FileDelta applyDelta(FileDelta delta, boolean leader, byte[] data) {
        Log.info("Applying delta: %s".formatted(delta));

        var uf = userFiles.computeIfAbsent(delta.getUserId(), (k) -> new UserFiles());
        var fileId = fileId(delta.getFilename(), delta.getUserId());

        if (delta.isRemoved()) { // remove file
            // remove it
            var info = files.remove(fileId);

            // remove from user
            uf.owned().remove(fileId);

            // remove from shares
            this.removeSharesOfFile(info);

            // set file counts
            info.uris().forEach(u -> getFileCounts(URI.create(u), false).numFiles().decrementAndGet());

            /* if (leader) TODO garbage collect (tell files servers that file was deleted) */

        }
        else {
            var file = files.computeIfAbsent(fileId, id -> {
                var f = new ExtendedFileInfo();
                f.fileId = fileId;
                f.info = new FileInfo();
                f.uris = new HashSet<>();
                return f;
            });
            var info = file.info();

            // set new owner and file name
            info.setOwner(delta.getUserId());
            info.setFilename(delta.getFilename());

            // set uris
            Set<String> failedAdds = new HashSet<>();
            delta.getAddedURIs().forEach(u -> {
                if (file.uris.add(u))
                    getFileCounts(URI.create(u), true).numFiles().incrementAndGet();
                if (leader) {
                    var res = FilesClients.get(URI.create(u)).writeFile(fileId, data, Token.get());
                    if (!res.isOK()) {
                        Log.warning("Failed at writing file to url %s. Status code: %s\nReason: %s"
                                .formatted(u, res.error(), res.errorValue()));
                        failedAdds.add(u);
                    }
                }
            });
            delta.getRemovedURIs().forEach(u -> {
                if (file.uris.remove(u)) {
                    getFileCounts(URI.create(u), false).numFiles().decrementAndGet();
                }
            });
            /* if (!delta.removedURIs().isEmpty() && leader) TODO garbage collect (tell files server new URI list) */


            // add shares
            delta.getAddedShares().forEach(s -> {
                var share_uf = userFiles.computeIfAbsent(s, (k) -> new UserFiles());
                synchronized (share_uf) {
                    share_uf.shared().add(fileId);
                    file.info().getSharedWith().add(s);
                }
            });

            delta.getRemovedShares().forEach(s -> {
                var share_uf = userFiles.computeIfAbsent(s, (k) -> new UserFiles());
                synchronized (share_uf) {
                    share_uf.shared().remove(fileId);
                    file.info().getSharedWith().remove(s);
                }
            });

            Log.fine("Final file: %s".formatted(file));
            info.setFileURL(String.format("%s/files/%s", file.uris().stream().findFirst().orElse(""), fileId));
            files.put(fileId, file);

            uf.owned().add(fileId);

            if (!failedAdds.isEmpty())
                return new FileDelta(delta.getUserId(), delta.getFilename(), false, null, failedAdds, null, null);
        }
        return null;

    }

    public String getBestMatchURI(String userId, String filename) {
        synchronized (userFiles.get(userId)) {
            var fileId = fileId(filename, userId);

            var bestMatch = files.get(fileId).uris()
                    .stream()
                    .map(URI::create)
                    .filter(FilesClients.all()::contains)
                    .findFirst().get();

            return String.format("%s/files/%s", bestMatch, fileId);

        }
    }

    /**
     * @param filename the name of the file to delete
     * @param userId   the name id of the owner of the file
     * @param password the password of the owner of the file
     * @return a pair that signifies the new state of the file
     */
    public Result<FileDelta> deleteFile(String filename, String userId, String password) {
        if (badParam(filename) || badParam(userId))
            return error(BAD_REQUEST);

        var fileId = fileId(filename, userId);

        var file = files.get(fileId);
        if (file == null)
            return error(NOT_FOUND);

        var user = getUser(userId, password);
        if (!user.isOK())
            return error(user.error());

        return ok(new FileDelta(userId, filename, true, null, null, null, null));
    }

    public Result<FileDelta> shareFile(String filename, String userId, String userIdShare, String password) {
        if (badParam(filename) || badParam(userId) || badParam(userIdShare))
            return error(BAD_REQUEST);

        var fileId = fileId(filename, userId);

        var file = files.get(fileId);
        if (file == null || getUser(userIdShare, "").error() == NOT_FOUND)
            return error(NOT_FOUND);

        var user = getUser(userId, password);
        if (!user.isOK())
            return error(user.error());

        return ok(new FileDelta(userId, filename, false, null, null, Set.of(userIdShare), null));
    }

    public Result<FileDelta> unshareFile(String filename, String userId, String userIdShare, String password) {
        if (badParam(filename) || badParam(userId) || badParam(userIdShare))
            return error(BAD_REQUEST);

        var fileId = fileId(filename, userId);

        var file = files.get(fileId);
        if (file == null || getUser(userIdShare, "").error() == NOT_FOUND)
            return error(NOT_FOUND);

        var user = getUser(userId, password);
        if (!user.isOK())
            return error(user.error());

        return ok(new FileDelta(userId, filename, false, null, null, null, Set.of(userIdShare)));
    }

    public Result<byte[]> getFile(String filename, String userId, String accUserId, String password) {
        if (badParam(filename))
            return error(BAD_REQUEST);

        var fileId = fileId(filename, userId);
        var file = files.get(fileId);
        if (file == null)
            return error(NOT_FOUND);

        var user = getUser(accUserId, password);
        if (!user.isOK())
            return error(user.error());

        if (!file.info().hasAccess(accUserId))
            return error(FORBIDDEN);

        return redirect(getBestMatchURI(userId, filename));
    }

    public Result<List<FileInfo>> lsFile(String userId, String password) {
        if (badParam(userId))
            return error(BAD_REQUEST);

        var user = getUser(userId, password);
        if (!user.isOK())
            return error(user.error());

        var uf = userFiles.getOrDefault(userId, new UserFiles());
        synchronized (uf) {
            var infos = Stream.concat(uf.owned().stream(), uf.shared().stream()).map(f -> files.get(f).info())
                    .collect(Collectors.toSet());

            return ok(new ArrayList<>(infos));
        }
    }

    public static String fileId(String filename, String userId) {
        return userId + JavaFiles.DELIMITER + filename;
    }

    protected static boolean badParam(String str) {
        return str == null || str.length() == 0;
    }

    protected Result<User> getUser(String userId, String password) {
        try {
            return users.get(new UserInfo(userId, password));
        } catch (Exception x) {
            x.printStackTrace();
            return error(ErrorCode.INTERNAL_ERROR);
        }
    }

    public Result<List<FileDelta>> deleteUserFiles(String userId) {
        List<FileDelta> deltas = new LinkedList<>();

        var uf = userFiles.get(userId);
        synchronized (uf) {
            users.invalidate(new UserInfo(userId, ""));

            var fileIds = userFiles.remove(userId);
            if (fileIds != null)
                for (var id : fileIds.owned()) {
                    String[] tokens = id.split(Pattern.quote(JavaFiles.DELIMITER));
                    deltas.add(new FileDelta(tokens[0], tokens[1]));
                }
        }

        return Result.ok(deltas);
    }

    public void invalidateUser(String userId) {
        users.invalidate(new UserInfo(userId, ""));
    }

    public void removeSharesOfFile(ExtendedFileInfo file) {
        for (var userId : file.info().getSharedWith())
            userFiles.getOrDefault(userId, new UserFiles()).shared().remove(file.fileId());
    }

    protected Queue<URI> orderCandidateFileServers(ExtendedFileInfo file) {
        int MAX_SIZE = 4;
        Queue<URI> result = new ArrayDeque<>();
        List<URI> reachableServers = FilesClients.all();

        if (file != null) {
            file.uris().stream()
                    .map(URI::create)
                    .filter(u -> !result.contains(u))
                    .filter(reachableServers::contains)
                    .map(u -> getFileCounts(u, false))
                    .sorted(FileCounts::ascending)
                    .limit(MAX_SIZE)
                    .map(FileCounts::uri)
                    .forEach(result::add);
        }

        FilesClients.all()
                .stream()
                .filter(u -> !result.contains(u))
                .map(u -> getFileCounts(u, false))
                .sorted(FileCounts::ascending)
                .limit(MAX_SIZE)
                .map(FileCounts::uri)
                .forEach(result::add);

        while (result.size() < MAX_SIZE)
            result.add(result.peek());

        Log.info("Candidate files servers: " + result + "\n");
        return result;
    }

    protected FileCounts getFileCounts(URI uri, boolean create) {
        if (create)
            return fileCounts.computeIfAbsent(uri, FileCounts::new);
        else
            return fileCounts.getOrDefault(uri, new FileCounts(uri));
    }


    static class ExtendedFileInfo {

        private String fileId;
        private Set<String> uris;
        private FileInfo info;

        public ExtendedFileInfo() {

        }

        public String fileId() {
            return fileId;
        }

        public FileInfo info() {
            return info;
        }

        public Set<String> uris() {
            return Set.copyOf(uris);
        }

        @Override
        public String toString() {
            return "ExtendedFileInfo{" +
                    "fileId='" + fileId + '\'' +
                    ", uris=" + uris +
                    ", info=" + info +
                    '}';
        }
    }

    static record UserFiles(Set<String> owned, Set<String> shared) {

        UserFiles() {
            this(ConcurrentHashMap.newKeySet(), ConcurrentHashMap.newKeySet());
        }
    }

    static record FileCounts(URI uri, AtomicLong numFiles) {
        FileCounts(URI uri) {
            this(uri, new AtomicLong(0L));
        }

        static int ascending(FileCounts a, FileCounts b) {
            return Long.compare(a.numFiles().get(), b.numFiles().get());
        }
    }

    static record UserInfo(String userId, String password) {
    }

}
