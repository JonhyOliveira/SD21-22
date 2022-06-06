package tp1.impl.servers.common;


import tp1.api.FileInfo;
import tp1.api.service.java.Directory;
import tp1.api.service.java.Result;
import tp1.impl.clients.rest.RestDirectoryURIFactory;
import tp1.impl.servers.common.replication.LeaderElection;
import tp1.impl.servers.common.replication.ReplicationManager;
import tp1.impl.servers.common.replication.Version;
import util.Token;

import java.net.URI;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import static tp1.api.service.java.Result.*;
import static tp1.impl.clients.Clients.DirectoryClients;

public class JavaDirectorySynchronizer implements Directory {

    private final static Logger Log = Logger.getLogger(JavaDirectorySynchronizer.class.getName());
    private static final int SUPPORTED_FAILS = 1;

    private final JavaDirectoryState state;
    private final ReplicationManager replicationManager;

    public JavaDirectorySynchronizer(ReplicationManager replicationManager) {
        this.replicationManager = replicationManager;
        state = new JavaDirectoryState();
    }

    @Override
    public Result<FileInfo> writeFile(String filename, byte[] data, String userId, String password) {

        if (replicationManager.election().amElected()) {
            var deltaResult = state.writeFile(filename, userId, password);

            if (!deltaResult.isOK())
                return error(deltaResult.error(), deltaResult.errorValue());

            Log.info("Calculated delta: %s".formatted(deltaResult));
            applyDelta(deltaResult.value(), (byte[]) null);
            Log.info("Done applying delta");

            var uf = state.userFiles.get(userId);
            synchronized (uf) {
                var fileId = JavaDirectoryState.fileId(filename, userId);
                return ok(state.files.get(fileId).info());
            }
        }
        else {
            var leader = replicationManager.election().leader();
            var uri = new RestDirectoryURIFactory(leader.serviceURI())
                    .forWriteFile(filename, data, userId, password).toString();

            Log.info("Redirecting to %s\nto leader %s".formatted(uri, leader));
            return redirect(uri);
        }
    }

    @Override
    public Result<Void> deleteFile(String filename, String userId, String password) {
        if (replicationManager.election().amElected()) {
            var deltaResult = state.deleteFile(filename, userId, password);

            if (!deltaResult.isOK())
                return error(deltaResult.error(), deltaResult.errorValue());

            applyDelta(deltaResult.value(), (byte[]) null);

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
        if (replicationManager.election().amElected()) {
            var deltaResult = state.shareFile(filename, userId, userIdShare, password);

            if (!deltaResult.isOK())
                return error(deltaResult.error(), deltaResult.errorValue());

            applyDelta(deltaResult.value(), (byte[]) null);

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
        if (replicationManager.election().amElected()) {
            var deltaResult = state.unshareFile(filename, userId, userIdShare, password);

            if (!deltaResult.isOK())
                return error(deltaResult.error(), deltaResult.errorValue());

            applyDelta(deltaResult.value(), (byte[]) null);

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
    public Result<Version> getVersion(String token) {
        return ok(replicationManager.version());
    }

    @Override
    public Result<Void> applyDelta(JavaDirectoryState.FileDelta delta, String token) {
        if (replicationManager.election().amElected())
            throw new IllegalStateException("How dare you?!"); // a leader should never receive this request
        else {
            // TODO check token
            Log.info("Got delta");
            applyDelta(delta, (byte[]) null);
            return ok();
        }
    }

    private synchronized JavaDirectoryState.FileDelta applyDelta(JavaDirectoryState.FileDelta delta, byte[] data) {
        if (replicationManager.election().amElected()) {

            CountDownLatch latch = new CountDownLatch(SUPPORTED_FAILS);
            var sequencedDelta = new SequencedFileDelta(replicationManager.version().next(), latch, delta);

            // send delta to followers
            replicationManager.election().followers()
                    .stream()
                    .map(LeaderElection.Node::serviceURI)
                    .map(this::getFollowerExecutor)
                    .forEach(fExec -> fExec.submit(sequencedDelta));

            try {
                // wait for minimum of followers to respond
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // execute locally
            return state.applyDelta(delta, true, data);
        }
        else {
            JavaDirectoryState.FileDelta failDelta;
            if ((failDelta = state.applyDelta(delta, false, null)) != null) // this should only update state, should never
                throw new IllegalStateException("What did you do? What this?:\n%s".formatted(failDelta)); // ask other replicas to undo

            return null;
        }
    }

    private FollowerExecutor getFollowerExecutor(URI serviceURI) {
        return followers.computeIfAbsent(serviceURI, FollowerExecutor::new);
    }

    private final Map<URI, FollowerExecutor> followers = new HashMap<>();

    record SequencedFileDelta(Version sequencingVersion, CountDownLatch done, JavaDirectoryState.FileDelta deltaToExecute) implements Comparable<SequencedFileDelta> {

        @Override
        public int compareTo(SequencedFileDelta o) {
            return sequencingVersion.compareTo(o.sequencingVersion);
        }

    }

    static abstract class SequencedDeltaApplier implements Runnable {

        private static final Logger Log = Logger.getLogger(SequencedDeltaApplier.class.getName());

        protected static final int WAIT_PERIOD = 100;

        protected final PriorityQueue<SequencedFileDelta> toExecute = new PriorityQueue<>();

        @Override
        public void run() {
            for (;;) {
                SequencedFileDelta deltaToExecute = toExecute.poll();
                if (deltaToExecute == null) {
                    synchronized (this) {
                        try {
                            this.wait(WAIT_PERIOD);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            return; // stop execution
                        }
                    }
                    continue;
                }

                Log.info("Applying delta sequence with %s".formatted(deltaToExecute.sequencingVersion()));
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

    static class FollowerExecutor extends SequencedDeltaApplier {

        private final URI serviceURI;

        public FollowerExecutor(URI serviceURI) {
            this.serviceURI = serviceURI;
        }

        @Override
        protected boolean apply(SequencedFileDelta delta) {
            var res = DirectoryClients.get(serviceURI).applyDelta(delta.deltaToExecute(), Token.get());
            if (res.isOK())
                delta.done().countDown();
            else if (!res.error().equals(Result.ErrorCode.TIMEOUT)) {
                throw new IllegalStateException(("""
                            Apply delta on follower %s got error code: %s
                            Reason: %s
                            What do?""")
                        .formatted(serviceURI, res.error(), res.errorValue()));
            }
            else // Down or overloaded -> wait for it to come back
                return true;

            return false;
        }
    }

}
