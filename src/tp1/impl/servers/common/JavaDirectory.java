package tp1.impl.servers.common;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import tp1.api.FileDelta;
import tp1.api.FileInfo;
import tp1.api.User;
import tp1.api.service.java.Directory;
import tp1.api.service.java.Result;
import tp1.impl.servers.common.replication.DirectoryOperation;
import util.Token;
import util.kafka.KafkaSubscriber;
import util.kafka.RecordProcessor;

import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static tp1.api.service.java.Result.*;
import static tp1.api.service.java.Result.ErrorCode.*;
import static tp1.impl.clients.Clients.FilesClients;
import static tp1.impl.clients.Clients.UsersClients;

public class JavaDirectory implements Directory, RecordProcessor {

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

    final static Logger Log = Logger.getLogger(JavaDirectory.class.getName());
    final ExecutorService executor = Executors.newCachedThreadPool();

    final Map<String, ExtendedFileInfo> files = new ConcurrentHashMap<>();
    final Map<String, UserFiles> userFiles = new ConcurrentHashMap<>();
    final Map<URI, FileCounts> fileCounts = new ConcurrentHashMap<>();

    {
        KafkaSubscriber.createSubscriber("kafka:9092", List.of(DirectoryOperation.NAMESPACE), "earliest")
                .start(false, this);
    }

    @Override
    public Result<FileInfo> writeFile(String filename, byte[] data, String userId, String password) {

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
                return ok(file.info());
            }
        }

        return error(BAD_REQUEST);

    }


    @Override
    public Result<Void> deleteFile(String filename, String userId, String password) {
        if (badParam(filename) || badParam(userId))
            return error(BAD_REQUEST);

        var fileId = fileId(filename, userId);

        var file = files.get(fileId);
        if (file == null)
            return error(NOT_FOUND);

        var user = getUser(userId, password);
        if (!user.isOK())
            return error(user.error());

        var uf = userFiles.getOrDefault(userId, new UserFiles());
        synchronized (uf) {
            var info = files.remove(fileId);
            uf.owned().remove(fileId);

            executor.execute(() -> {
                this.removeSharesOfFile(info);
                // FilesClients.get(file.primaryURI()).deleteFile(fileId, password);
            });

            getFileCounts(info.primaryURI(), false).numFiles().decrementAndGet();
            if (file.backupURI() != null)
                getFileCounts(info.backupURI(), false).numFiles().decrementAndGet();
        }
        return ok();
    }

    @Override
    public Result<Void> shareFile(String filename, String userId, String userIdShare, String password) {
        if (badParam(filename) || badParam(userId) || badParam(userIdShare))
            return error(BAD_REQUEST);

        var fileId = fileId(filename, userId);

        var file = files.get(fileId);
        if (file == null || getUser(userIdShare, "").error() == NOT_FOUND)
            return error(NOT_FOUND);

        var user = getUser(userId, password);
        if (!user.isOK())
            return error(user.error());

        var uf = userFiles.computeIfAbsent(userIdShare, (k) -> new UserFiles());
        synchronized (uf) {
            uf.shared().add(fileId);
            file.info().getSharedWith().add(userIdShare);
        }

        return ok();
    }

    @Override
    public Result<Void> unshareFile(String filename, String userId, String userIdShare, String password) {
        if (badParam(filename) || badParam(userId) || badParam(userIdShare))
            return error(BAD_REQUEST);

        var fileId = fileId(filename, userId);

        var file = files.get(fileId);
        if (file == null || getUser(userIdShare, "").error() == NOT_FOUND)
            return error(NOT_FOUND);

        var user = getUser(userId, password);
        if (!user.isOK())
            return error(user.error());

        var uf = userFiles.computeIfAbsent(userIdShare, (k) -> new UserFiles());
        synchronized (uf) {
            uf.shared().remove(fileId);
            file.info().getSharedWith().remove(userIdShare);
        }

        return ok();
    }

    @Override
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
            Log.fine("Primary URI %s declared unresponsive. Switching 2 backup: %s".formatted(file.primaryURI(), file.backupURI()));
            file.switch2Backup();
        }

        return redirect(file.info().getFileURL());
    }

    @Override
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

    @Override
    public Result<Void> deleteUserFiles(String userId, String password, String token) {
        return error(INTERNAL_ERROR, "Not used");
    }

    @Override
    public Result<String> getVersion(String token) {
        return error(NOT_IMPLEMENTED);
    }

    @Override
    public Result<Void> applyDelta(String version, String token, FileDelta delta) {
        return error(NOT_IMPLEMENTED);
    }

    protected void deleteUserFiles(String userId) {
        users.invalidate(new UserInfo(userId, ""));

        var fileIds = userFiles.remove(userId);
        if (fileIds != null)
            for (var id : fileIds.owned()) {
                var file = files.remove(id);
                removeSharesOfFile(file);
                getFileCounts(file.primaryURI(), false).numFiles().decrementAndGet();
                if (file.backupURI() != null)
                    getFileCounts(file.backupURI(), false).numFiles().decrementAndGet();
            }
    }

    protected void removeSharesOfFile(ExtendedFileInfo file) {
        for (var userId : file.info().getSharedWith())
            userFiles.getOrDefault(userId, new UserFiles()).shared().remove(file.fileId());
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

    @Override
    public void onReceive(ConsumerRecord<String, String> r) {
        Log.info("GCFN %s".formatted(r));
        var op = DirectoryOperation.Operation.fromRecord(r);

        if (op.operationType().equals(DirectoryOperation.OperationType.DELETE_USER))
            deleteUserFiles(op.data());
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
}