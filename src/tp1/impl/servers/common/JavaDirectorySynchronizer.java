package tp1.impl.servers.common;


import com.google.gson.Gson;
import tp1.api.FileDelta;
import tp1.api.FileInfo;
import tp1.api.service.java.Directory;
import tp1.api.service.java.Result;
import tp1.impl.clients.rest.RestDirectoryURIFactory;
import tp1.impl.servers.common.replication.LeaderElection;
import tp1.impl.servers.common.replication.ReplicationManager;
import tp1.impl.servers.common.replication.Version;
import util.Json;
import util.Token;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static tp1.api.service.java.Result.*;
import static tp1.impl.clients.Clients.DirectoryClients;

public class JavaDirectorySynchronizer implements Directory {

    private final static Logger Log = Logger.getLogger(JavaDirectorySynchronizer.class.getName());
    private static final int SUPPORTED_FAILS = 1;

    private final JavaDirectoryState state;
    private final ReplicationManager replicationManager;
    private static Gson json = Json.getInstance();

    private final Map<Version, FileDelta> executedOperations = new ConcurrentHashMap<>();

    public JavaDirectorySynchronizer(ReplicationManager replicationManager) {
        this.replicationManager = replicationManager;
        state = new JavaDirectoryState();
    }

    @Override
    public Result<FileInfo> writeFile(String filename, byte[] data, String userId, String password) {

        if (replicationManager.amLeader()) {
            var deltaResult = state.writeFile(filename, userId, password);

            if (!deltaResult.isOK())
                return error(deltaResult.error(), deltaResult.errorValue());

            Log.info("Calculated delta: %s".formatted(deltaResult));
            applyDelta(null, deltaResult.value(), data);
            Log.info("Done applying delta");

            return ok(state.files.get(JavaDirectoryState.fileId(filename, userId)).info());
        }
        else {
            var leader = replicationManager.election().leader();
            var uri = new RestDirectoryURIFactory(leader.serviceURI())
                    .forWriteFile(filename, data, userId, password).toString();

            Log.info("Redirecting to %s\nLeader: %s".formatted(uri, leader));
            return redirect(uri);
        }
    }

    @Override
    public Result<Void> deleteFile(String filename, String userId, String password) {
        if (replicationManager.amLeader()) {
            var deltaResult = state.deleteFile(filename, userId, password);

            if (!deltaResult.isOK())
                return error(deltaResult.error(), deltaResult.errorValue());

            applyDelta(null, deltaResult.value(), null);

            return ok();
        }
        else {
            var leader = replicationManager.election().leader();
            return redirect(new RestDirectoryURIFactory(leader.serviceURI())
                    .forDeleteFile(filename, userId, password).toString());
        }
    }

    @Override
    public Result<Void> shareFile(String filename, String userId, String userIdShare, String password) {
        if (replicationManager.amLeader()) {
            var deltaResult = state.shareFile(filename, userId, userIdShare, password);

            if (!deltaResult.isOK())
                return error(deltaResult.error(), deltaResult.errorValue());

            applyDelta(null, deltaResult.value(), null);

            return ok();
        }
        else {
            var leader = replicationManager.election().leader();
            return redirect(new RestDirectoryURIFactory(leader.serviceURI())
                    .forShareFile(filename, userId, userIdShare, password).toString());
        }
    }

    @Override
    public Result<Void> unshareFile(String filename, String userId, String userIdShare, String password) {
        if (replicationManager.amLeader()) {
            var deltaResult = state.unshareFile(filename, userId, userIdShare, password);

            if (!deltaResult.isOK())
                return error(deltaResult.error(), deltaResult.errorValue());

            applyDelta(null, deltaResult.value(), null);

            return ok();
        }
        else {
            var leader = replicationManager.election().leader();
            return redirect(new RestDirectoryURIFactory(leader.serviceURI())
                    .forUnShareFile(filename, userId, userIdShare, password).toString());
        }
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
    public Result<String> getVersion(String token) {
        // TODO check token
        return ok(json.toJson(replicationManager.version()));
    }

    @Override
    public Result<Void> applyDelta(String version, String token, FileDelta delta) {
        if (replicationManager.amLeader())
            throw new IllegalStateException("How dare you?!"); // a leader should never receive this request
        else {
            // TODO check token
            Log.info("Got delta %s\nwith version %s".formatted(delta, version));
            applyDelta(json.fromJson(version, Version.class), delta, null);
            return ok();
        }
    }

    private FileDelta applyDelta(Version version, FileDelta delta, byte[] data) {
        FileDelta failDelta;
        SequencedFileDelta sequencedDelta;

        if (replicationManager.amLeader()) {
            CountDownLatch latch = new CountDownLatch(SUPPORTED_FAILS);
            sequencedDelta = new SequencedFileDelta(replicationManager.version().next(replicationManager.replicaID()), latch, delta);

            // send delta to followers
            var followersURIs = replicationManager.followers()
                    .stream()
                    .map(LeaderElection.Candidate::serviceURI)
                    .collect(Collectors.toList());
            if (followersURIs.size() < SUPPORTED_FAILS)
                Log.severe("Not enough replicas to support service failure.");

            Log.info("Sending delta %s to followers: %s".formatted(sequencedDelta.sequencingVersion(), followersURIs));

            followersURIs
                    .stream()
                    .map(this::getFollowerExecutor)
                    .forEach(fExec -> {
                        fExec.submit(sequencedDelta);
                    });

            try {
                // wait for minimum of followers to respond
                Log.finer("Waiting for followers to respond...");
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        else {
            sequencedDelta = new SequencedFileDelta(version, null, delta);
        }

        failDelta = state.applyDelta(delta, true, data);

        if (!replicationManager.amLeader() && failDelta != null) // followers should never ask other replicas to undo
            throw new IllegalStateException("What did you do? What this? D: :\n%s".formatted(failDelta));

        // everything went good, record delta
        executedOperations.put(sequencedDelta.sequencingVersion(), sequencedDelta.deltaToExecute());
        replicationManager.setVersion(sequencedDelta.sequencingVersion());
        return failDelta;
    }

    private FollowerExecutorThread getFollowerExecutor(URI serviceURI) {
        return followers.computeIfAbsent(serviceURI, FollowerExecutorThread::new);
    }

    private final Map<URI, FollowerExecutorThread> followers = new HashMap<>();

    record SequencedFileDelta(Version sequencingVersion, CountDownLatch done, FileDelta deltaToExecute) implements Comparable<SequencedFileDelta> {

        @Override
        public int compareTo(SequencedFileDelta o) {
            return sequencingVersion.compareTo(o.sequencingVersion);
        }

    }

    static abstract class SequencedDeltaApplierThread extends Thread {

        private static final Logger Log = Logger.getLogger(SequencedDeltaApplierThread.class.getName());

        protected static final int WAIT_PERIOD = 100;

        protected final PriorityQueue<SequencedFileDelta> toExecute = new PriorityQueue<>();

        @Override
        public void run() {
            for (;;) {
                SequencedFileDelta deltaToExecute = toExecute.poll();
                if (deltaToExecute == null) {
                    try {
                        Thread.sleep(WAIT_PERIOD);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return; // stop execution
                    }
                    continue;
                }

                if (apply(deltaToExecute))
                    return;
            }
        }

        public synchronized void submit(SequencedFileDelta deltaToExecute) {
            this.toExecute.add(deltaToExecute);
            this.notify();
        }

        /**
         * Applies the delta
         * @param delta delta to apply
         * @return if execution should continue or not
         */
        protected abstract boolean apply(SequencedFileDelta delta);
    }

    static class FollowerExecutorThread extends SequencedDeltaApplierThread {

        private final URI serviceURI;

        public FollowerExecutorThread(URI serviceURI) {
            this.serviceURI = serviceURI;
            this.start();
        }

        @Override
        protected boolean apply(SequencedFileDelta delta) {
            Log.fine("Sending delta %s to %s".formatted(json.toJson(delta.sequencingVersion()), serviceURI));
            var res = DirectoryClients.get(serviceURI).applyDelta(json.toJson(delta.sequencingVersion()), Token.get(), delta.deltaToExecute());
            if (res.isOK())
                delta.done().countDown();
            else if (!res.error().equals(Result.ErrorCode.TIMEOUT)) {
                throw new IllegalStateException(("""
                            Apply delta on follower %s got error code: %s
                            Reason: %s
                            What do?""")
                        .formatted(serviceURI, res.error(), res.errorValue()));
            }
            else {// Down or overloaded -> wait for it to come back
                Log.fine("Got Timeout from %s".formatted(serviceURI));
                return true;
            }

            return false;
        }
    }

    record DeltaToApply(SequencedFileDelta delta, Collection<FollowerExecutorThread> executors) {

        public void send() {
            executors.forEach(e -> e.apply(delta));
        }

        public boolean isDone() {
            return delta.done().getCount() <= 0;
        }

    }

}
