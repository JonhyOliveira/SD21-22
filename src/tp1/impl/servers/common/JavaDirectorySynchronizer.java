package tp1.impl.servers.common;


import com.google.gson.Gson;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.zookeeper.CreateMode;
import tp1.api.FileInfo;
import tp1.api.service.java.Directory;
import tp1.api.service.java.Result;
import tp1.impl.servers.common.replication.DirectoryOperation;
import tp1.impl.servers.common.replication.IOperation;
import tp1.impl.servers.common.replication.Version;
import util.Json;
import util.kafka.KafkaPublisher;
import util.kafka.KafkaSubscriber;
import util.kafka.KafkaUtils;
import util.kafka.RecordProcessor;
import util.zookeeper.Zookeeper;

import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Logger;

public class JavaDirectorySynchronizer implements Directory, RecordProcessor {

    final static Logger Log = Logger.getLogger(JavaDirectorySynchronizer.class.getName());

    private final JavaDirectoryState state = new JavaDirectoryState();
    final Gson json = Json.getInstance();

    private final KafkaPublisher publisher = KafkaPublisher.createPublisher("kafka:9092");
    private final String replicaId;
    private final Long currentVersion;

    public JavaDirectorySynchronizer() {


        currentVersion = -1L;
    }

    @Override
    public Result<FileInfo> writeFile(String filename, byte[] data, String userId, String password) {

    }

    @Override
    public Result<Void> deleteFile(String filename, String userId, String password) {

    }

    @Override
    public Result<Void> shareFile(String filename, String userId, String userIdShare, String password) {

    }

    @Override
    public Result<Void> unshareFile(String filename, String userId, String userIdShare, String password) {

    }

    @Override
    public Result<byte[]> getFile(String filename, String userId, String accUserId, String password) {
        return state.getFile(filename, userId, accUserId, password);
    }

    @Override
    public Result<List<FileInfo>> lsFile(String userId, String password) {
        return state.lsFile(userId, password);
    }

    @Override
    public Result<Void> deleteUserFiles(String userId, String password, String token) {
        throw new RuntimeException("Deprecated - Use kafka");
    }

    @Override
    public void onReceive(ConsumerRecord<String, String> r) {
        Log.info("GCFN %s".formatted(r));

        var op = DirectoryOperation.Operation.fromRecord(r);
        var opVersion = op.version();

        Log.info("His name is %s. He's not the law, but I'll look over his offer.\n%s\n".formatted(opVersion.replicaID(), op));
        if (opVersion.v() < 0) // invalid, must be current
            op = new DirectoryOperation.Operation(op.operationType(), currentVersion.copy(), op.data());

        switch (op.operationType()) {
            case UPDATE_FILE -> processIncomingOp(op);
            case DELETE_USER -> { // decompose into file updates
                var user = op.data();
                var x = op; // required final
                state.userFiles.get(op.data()).owned().stream()
                        .map(s -> new DirectoryOperation.Operation(DirectoryOperation.OperationType.UPDATE_FILE,
                                (x.version() != null) ? x.version() : currentVersion, json.toJson(new JavaDirectoryState.FileState(s, null)))
                        ).forEach(this::processIncomingOp);
            }
        }
    }

    /**
     * Processes an incoming operation
     *
     * @param op the operation
     */
    private void processIncomingOp(DirectoryOperation.Operation op) {
        if (op.operationType().equals(DirectoryOperation.OperationType.UPDATE_FILE)) {
            var opState = json.fromJson(op.data(), JavaDirectoryState.FileState.class);
            var opRecord = OperationRecord.fromOperation(op, opState);
            synchronized (currentVersion) {
                if (currentVersion.compareTo(op.version()) <= 0) { // op succeeds current version
                    currentVersion.set(op.version());
                    currentVersion.notifyAll();
                }
                 else { // check if applied operations overwrite
                    var modifying = opState.fileId();
                    var intermediateOperations =
                            appliedOperations.tailSet(opRecord);
                    for (OperationRecord currOpRecord : intermediateOperations)
                        if (modifying.equals(currOpRecord.modifiedFile().fileId()))
                            return; // if so, do nothing
                }

                appliedOperations.add(opRecord);
                state.setFileAtomic(opState.fileId(), opState.fileInfo());
            }
        }

    }

    /**
     * Generates operation for this state and sends it out
     *
     * @param state the file state to propagate
     */
    private synchronized void propagateState(JavaDirectoryState.FileState state) {
            var op = newOperation(DirectoryOperation.OperationType.UPDATE_FILE, json.toJson(state));
            appliedOperations.add(OperationRecord.fromOperation(op, state));
            Log.info("Sending operation %s".formatted(op.toRecord()));
            publisher.publish(op.toRecord());
            Log.info("Propagated operation %s".formatted(op.toRecord()));
    }

    /**
     * Generates a new operation DirectoryOperation
     *
     * @param operationType the type of the operation
     * @param data          the data associated with the operation
     * @return the generated operataion
     */
    private DirectoryOperation.Operation newOperation(DirectoryOperation.OperationType operationType, String data) {
        synchronized (currentVersion) {
            this.currentVersion.next(this.replicaId);
            currentVersion.notifyAll();
            return operationType.toOperation(this.currentVersion.copy(), data);
        }
    }

    /**
     * Waits for the current version to reach the threshold
     *
     * @param v the version threshold
     */
    public void waitForVersion(Version v) {
        this.waitForVersion(v, Long.MAX_VALUE);
    }

    /**
     * Waits for the current version to reach the threshold
     *
     * @param v          the version threshold
     * @param waitPeriod how long to wait
     */
    public synchronized void waitForVersion(Version v, long waitPeriod) {
        while (currentVersion.compareTo(v) < 0) {
            try {
                currentVersion.wait(waitPeriod);
            } catch (InterruptedException e) {
                Log.info("Exception while waiting for version");
                e.printStackTrace();
            }
        }
    }

    /**
     * The current directory version
     *
     * @return the version
     */
    public Version getCurrentVersion() {
        return currentVersion;
    }

    record OperationRecord(Version version, JavaDirectoryState.FileState modifiedFile) {
        public static OperationRecord fromOperation(IOperation operation, JavaDirectoryState.FileState state) {
            return new OperationRecord(operation.version().copy(), state.fileId());
        }

        public OperationRecord(Version version, String fileId) {
            this(version, new JavaDirectoryState.FileState(fileId, null));
        }
    }

}
