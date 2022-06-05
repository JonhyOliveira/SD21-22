package tp1.impl.servers.common;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import tp1.api.FileInfo;
import tp1.api.User;
import tp1.api.service.java.Result;
import util.Token;

import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static tp1.api.service.java.Result.*;
import static tp1.api.service.java.Result.ErrorCode.*;
import static tp1.impl.clients.Clients.FilesClients;
import static tp1.impl.clients.Clients.UsersClients;

public class JavaDirectoryState {

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

    public Result<FileState> writeFile(String filename, byte[] data, String userId, String password) {

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
            URI uri1 = null, uri2 = null;
            for (var uri : orderCandidateFileServers(file)) {
                var result = FilesClients.get(uri).writeFile(fileId, data, Token.get());
                if (result.isOK()) { // find first 2 distinct reachable uris (one main, one backup)
                    if (uri1 == null)
                        uri1 = uri;
                    else if (!uri.equals(uri1)) {
                        uri2 = uri;
                        break;
                    }
                } else
                    Log.info(String.format("Files.writeFile(...) to %s failed with: %s \n", uri, result));
            }
            if (uri1 != null) {
                info.setOwner(userId);
                info.setFilename(filename);
                info.setFileURL(String.format("%s/files/%s", uri1, fileId));
                files.put(fileId, file = new ExtendedFileInfo(uri1, uri2, fileId, info));
                if (uf.owned().add(fileId)) {
                    getFileCounts(file.primaryURI(), true).numFiles().incrementAndGet();
                    if (file.backupURI() != null)
                        getFileCounts(file.backupURI(), true).numFiles().incrementAndGet();
                }
                return ok(new FileState(file.fileId(), file));
            }
        }

        return error(BAD_REQUEST);

    }

    /**
     * @param filename the name of the file to delete
     * @param userId   the name id of the owner of the file
     * @param password the password of the owner of the file
     * @return a pair that signifies the new state of the file
     */
    public Result<FileState> deleteFile(String filename, String userId, String password) {
        if (badParam(filename) || badParam(userId))
            return error(BAD_REQUEST);

        var fileId = fileId(filename, userId);

        var file = files.get(fileId);
        if (file == null)
            return error(NOT_FOUND);

        var user = getUser(userId, password);
        if (!user.isOK())
            return error(user.error());

        deleteFile(userId, fileId);
        return ok(new FileState(fileId, null));
    }

    public Result<FileState> shareFile(String filename, String userId, String userIdShare, String password) {
        if (badParam(filename) || badParam(userId) || badParam(userIdShare))
            return error(BAD_REQUEST);

        var fileId = fileId(filename, userId);

        var file = files.get(fileId);
        if (file == null || getUser(userIdShare, "").error() == NOT_FOUND)
            return error(NOT_FOUND);

        var user = getUser(userId, password);
        if (!user.isOK())
            return error(user.error());

        shareFileWith(filename, userId, userIdShare);
        return ok(new FileState(fileId, file));
    }

    public Result<FileState> unshareFile(String filename, String userId, String userIdShare, String password) {
        if (badParam(filename) || badParam(userId) || badParam(userIdShare))
            return error(BAD_REQUEST);

        var fileId = fileId(filename, userId);

        var file = files.get(fileId);
        if (file == null || getUser(userIdShare, "").error() == NOT_FOUND)
            return error(NOT_FOUND);

        var user = getUser(userId, password);
        if (!user.isOK())
            return error(user.error());

        unsharedFileWith(filename, userId, userIdShare);

        return ok(new FileState(fileId, file));
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

        if (!FilesClients.all().contains(file.primaryURI())) {
            if (!FilesClients.all().contains(file.backupURI())) {
                Log.fine("Neither URIs are responsive. Uh oh.");
                return error(NOT_FOUND);
            }
            Log.fine("Primary URI %s declared unresponsive. Switching 2 backup: %s".formatted(file.primaryURI(), file.backupURI()));
            file.switch2Backup();
        }

        return redirect(file.info().getFileURL());
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

    public Result<List<FileState>> deleteUserFiles(String userId) {
        List<FileState> states = new LinkedList<>();

        var uf = userFiles.get(userId);
        synchronized (uf) {
            users.invalidate(new UserInfo(userId, ""));

            var fileIds = userFiles.remove(userId);
            if (fileIds != null)
                for (var id : fileIds.owned()) {
                    deleteFile(userId, id);
                    states.add(new FileState(id, null));
                }
        }

        return Result.ok(states);
    }

    public void invalidateUser(String userId) {
        users.invalidate(new UserInfo(userId, ""));
    }

    public void removeSharesOfFile(ExtendedFileInfo file) {
        for (var userId : file.info().getSharedWith())
            userFiles.getOrDefault(userId, new UserFiles()).shared().remove(file.fileId());
    }

    /**
     * Applies an atomic change to a file
     *
     * @param fileId   the file being changed
     * @param fileInfo the information related to the file being changed
     * @return true if the operation is valid, false otherwise
     */
    public boolean setFileAtomic(String fileId, ExtendedFileInfo fileInfo) {
        if (!fileId.equals(fileInfo.fileId()))
            return false;

        var userId = fileInfo.info().getOwner();

        var uf = userFiles.computeIfAbsent(userId, (k) -> new UserFiles());
        synchronized (uf) {
            var file = files.get(fileId); // current fileInfo

            // if we had a previous version, delete it
            if (file != null) {
                // remove it
                var info = files.remove(fileId);

                // remove from user
                uf.owned().remove(fileId);

                // remove from shares
                this.removeSharesOfFile(info);

                // set file counts
                if (info.primaryURI() != null)
                    getFileCounts(info.primaryURI(), false).numFiles().decrementAndGet();
                if (info.backupURI() != null)
                    getFileCounts(info.backupURI(), false).numFiles().decrementAndGet();

            }

            // now we can start from scratch
            if (fileInfo != null) {
                var info = fileInfo.info();
                file = fileInfo;

                // add it
                files.put(fileId, fileInfo);

                // add to user
                uf.owned().add(fileId);

                // add shares
                info.getSharedWith().forEach(s -> {
                    shareFileWith(info.getFilename(), info.getOwner(), s);
                });

                // set file counts
                if (file.primaryURI() != null)
                    getFileCounts(file.primaryURI(), true).numFiles().incrementAndGet();
                if (file.backupURI() != null)
                    getFileCounts(file.backupURI(), true).numFiles().incrementAndGet();
            }

        }
        return true;
    }

    // TODO operations

    protected void deleteFile(String userId, String fileId) {
        var uf = userFiles.computeIfAbsent(userId, (k) -> new UserFiles());
        synchronized (uf) {

            // remove it
            var info = files.remove(fileId);

            // remove from user
            uf.owned().remove(fileId);

            // remove from shares
            this.removeSharesOfFile(info);

            // set file counts
            if (info.primaryURI() != null)
                getFileCounts(info.primaryURI(), false).numFiles().decrementAndGet();
            if (info.backupURI() != null)
                getFileCounts(info.backupURI(), false).numFiles().decrementAndGet();
        }

    }

    // TODO
    public void shareFileWith(String filename, String userId, String userIdShare) {
        var fileId = fileId(filename, userId);
        var file = files.get(fileId);

        var uf = userFiles.computeIfAbsent(userIdShare, (k) -> new UserFiles());
        synchronized (uf) {
            uf.shared().add(fileId);
            file.info().getSharedWith().add(userIdShare);
        }
    }

    // TODO
    public void unsharedFileWith(String filename, String userId, String userIdShare) {
        var fileId = fileId(filename, userId);
        var file = files.get(fileId);

        var uf = userFiles.computeIfAbsent(userIdShare, (k) -> new UserFiles());
        synchronized (uf) {
            uf.shared().remove(fileId);
            file.info().getSharedWith().remove(userIdShare);
        }
    }

    protected Queue<URI> orderCandidateFileServers(ExtendedFileInfo file) {
        int MAX_SIZE = 4;
        Queue<URI> result = new ArrayDeque<>();

        if (file != null) {
            result.add(file.primaryURI());
            if (file.backupURI() != null)
                result.add(file.backupURI());
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

        private final String fileId;
        private URI primaryURI, backupURI;
        private final FileInfo info;

        ExtendedFileInfo(URI primaryURI, URI backupURI, String fileId, FileInfo info) {
            this.primaryURI = primaryURI;
            this.backupURI = backupURI;
            this.fileId = fileId;
            this.info = info;
        }

        public String fileId() {
            return fileId;
        }

        public URI primaryURI() {
            return primaryURI;
        }

        public URI backupURI() {
            return backupURI;
        }

        public FileInfo info() {
            return info;
        }

        public void switch2Backup() {
            var temp = primaryURI;
            primaryURI = backupURI;
            backupURI = temp;
            info.setFileURL(String.format("%s/files/%s", primaryURI, fileId));
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

    static record FileState(String fileId, ExtendedFileInfo fileInfo) {

        public boolean conflictsWith(FileState o) {
            if (this == o) return true;
            if (o == null) return false;

            return fileId.equals(o.fileId);
        }

        public FileState dummy() {
            return new FileState(fileId, null);
        }
    }

}
