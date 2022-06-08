package tp1.impl.servers.common;


import com.google.gson.Gson;
import tp1.api.FileDelta;
import tp1.api.FileInfo;
import tp1.api.service.java.Directory;
import tp1.api.service.java.Result;
import tp1.impl.clients.rest.RestDirectoryURIFactory;
import tp1.impl.servers.common.replication.*;
import util.Json;
import util.Token;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static tp1.api.service.java.Result.*;
import static tp1.impl.clients.Clients.DirectoryClients;

public class JavaDirectorySynchronizer implements Directory {

    private final static Logger Log = Logger.getLogger(JavaDirectorySynchronizer.class.getName());
    private static final int SUPPORTED_FAILS = 1;

    private final JavaDirectoryState state;
    private final ReplicationManager replicationManager;
    private static final Gson json = Json.getInstance();

    private final Set<SequencedFileDelta> executedOperations = new ConcurrentSkipListSet<>();
    private final SortedSet<SequencedFileDelta> unAppliedDeltas = new TreeSet<>();

    public JavaDirectorySynchronizer(String serviceURI) {
        this.replicationManager = new ReplicationManager(serviceURI,
                new LeaderElection.Config(this::onNewCandidate, this::onCandidateGone));
        state = new JavaDirectoryState();
        replicationManager.election().watchCandidates();
    }

    public void onCandidateGone(LeaderElection.Candidate candidate) {
        if (replicationManager.amLeader()) {
            Log.info("%s got owned".formatted(candidate.serviceURI()));
            followerExecutors.remove(candidate.serviceURI().toString()).interrupt();
        }

    }

    public void onNewCandidate(LeaderElection.Candidate candidate) {
        if (replicationManager.amLeader() && !candidate.equals(replicationManager.leader())) {
            synchronized (replicationManager) {
                var uri = candidate.serviceURI();

                var executor = new FollowerDeltaApplier(uri);
                followerExecutors.put(uri.toString(), executor);

                Log.info("New follower: %s".formatted(uri));
                /*var res = DirectoryClients.get(uri).getVersion(Token.get());
                if (!res.isOK())
                    throw new IllegalStateException("Follower down on new");*/

                // queue operations to update
                var replicaVersion = Version.ZERO_VERSION; // json.fromJson(res.value(), Version.class);
                Log.info("Got version %s from %s".formatted(replicaVersion, uri));

                executedOperations
                        .stream()
                        .filter(seqFileDelta -> canApplyDelta.compare(seqFileDelta.sequencingVersion(), replicaVersion) >= 0)
                        .sorted()
                        .forEach(delta -> {
                            Log.info("Queued delta: %s".formatted(delta));
                            executor.submit(delta);
                        });

                executor.start();

            }
        }
    }

    @Override
    public Result<FileInfo> writeFile(String filename, byte[] data, String userId, String password) {

        if (replicationManager.amLeader()) {
            var r = state.writeFile(filename, userId, password, data);
            var deltaResult = r.v1();

            if (!deltaResult.isOK())
                return error(deltaResult.error(), deltaResult.errorValue());

            Log.info("Calculated delta: %s".formatted(deltaResult.value()));
            if (deltaResult.value() != null)
                applyDelta(new SequencedFileDelta(replicationManager.version().nextCopy(replicationManager().replicaID()),
                        new CountDownLatch(SUPPORTED_FAILS), deltaResult.value(), data, r.v2()));
            else {
                r.v2().get();
            }

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
            var r = state.deleteFile(filename, userId, password);
            var deltaResult = r.v1();

            if (!deltaResult.isOK())
                return error(deltaResult.error(), deltaResult.errorValue());

            Log.info("Calculated delta: %s".formatted(deltaResult.value()));
            if (deltaResult.value() != null)
                applyDelta(new SequencedFileDelta(replicationManager.version().next(replicationManager.replicaID()),
                        new CountDownLatch(SUPPORTED_FAILS), deltaResult.value(), null, () -> {r.v2().run(); return null; }));
            else {
                r.v2().run();
            }
            Log.info("Done applying delta");

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

            Log.info("Calculated delta: %s".formatted(deltaResult.value()));
            if (deltaResult.value() != null)
                applyDelta(new SequencedFileDelta(replicationManager.version().next(replicationManager.replicaID()),
                                new CountDownLatch(SUPPORTED_FAILS), deltaResult.value(), null, null));
                        Log.info("Done applying delta");

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

            Log.info("Calculated delta: %s".formatted(deltaResult.value()));
            if (deltaResult.value() != null)
                applyDelta(new SequencedFileDelta(replicationManager.version().next(replicationManager.replicaID()),
                        new CountDownLatch(SUPPORTED_FAILS), deltaResult.value(), null, null));
            Log.info("Done applying delta");

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
        return ok(Json.getInstance().toJson(replicationManager.version()));
    }

    @Override
    public Result<String> applyDelta(String version, String token, FileDelta delta) {
        if (replicationManager.amLeader())
            throw new IllegalStateException("How dare you?!"); // a leader should never receive this request
        else {
            // TODO check token
            var dVersion = json.fromJson(version, Version.class);
            Log.info("Got delta %s\nwith version %s\nexpecting >= %s with same version number\n".formatted(delta, version, replicationManager.version().nextCopy()));
            if (delta != null)
                submitDelta(new SequencedFileDelta(dVersion, null, delta, null, null));

            return ok();
        }
    }

    public ReplicationManager replicationManager() {
        return replicationManager;
    }

    /**
     * Compares a delta version with the current version and returns 0 if it can be applied
     */
    private final Comparator<Version> canApplyDelta = (o1, o2) -> (int) (o1.getVersion() - o2.getVersion() - 1);;

    /**
     * Submits a delta executing it if possible. May result in the application of deltas that couldn't be applied until
     * now due to total order execution
     * @param fileDelta the sequenced file delta
     */
    private void submitDelta(SequencedFileDelta fileDelta) {
        synchronized (unAppliedDeltas) {
            unAppliedDeltas.add(fileDelta); // serialize it

            SequencedFileDelta unAppliedDelta;
            while (!unAppliedDeltas.isEmpty()) {
                unAppliedDelta = unAppliedDeltas.first();

                var comparison = canApplyDelta.compare(unAppliedDelta.sequencingVersion(), replicationManager.version());
                Log.info("Comparison: %d".formatted(comparison));
                if (comparison > 0) // have to wait to apply, put back
                    break; // since this set is ordered we know the next ones will have higher sequencing version
                else if (comparison == 0) // should be applied now
                    applyDelta(fileDelta);
                // else comparison < 0 -> total order execution violation, just discard and go next

                unAppliedDeltas.remove(unAppliedDelta);

            }

            Log.info("Submitted. Un-applied: %s".formatted(unAppliedDeltas));
        }

    }

    private synchronized FileDelta applyDelta(SequencedFileDelta fileDelta) {
        synchronized (replicationManager) {
            FileDelta failDelta = null;

            if (replicationManager.amLeader()) { // follower only needs to apply delta to state
                // send delta to followers
                if (followerExecutors.size() < SUPPORTED_FAILS) {
                    Log.severe("Not enough replicas to support service failure.");
                    this.unAppliedDeltas.add(fileDelta);
                    throw new IllegalStateException("Not enough replicas to support service failure.");
                }
                Log.info("Sending delta %s to followers: %s".formatted(fileDelta.sequencingVersion(),
                        followerExecutors.values().stream().map(FollowerDeltaApplier::serviceURI).collect(Collectors.toList())));

                followerExecutors.values().forEach(fExec -> fExec.submit(fileDelta));

                try {
                    // wait for minimum of followers to respond
                    Log.finer("Waiting for followers to respond...");
                    fileDelta.done().await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            state.applyDelta(fileDelta.deltaToExecute(), replicationManager.amLeader(), fileDelta.data());

            if (fileDelta.effect() != null)
                failDelta = fileDelta.effect().get();

            if (!replicationManager.amLeader() && failDelta != null) // followers should never ask other replicas to undo
                throw new IllegalStateException("What did you do? What this? D: :\n%s".formatted(failDelta));

            // everything went good, record delta
            executedOperations.add(fileDelta);
            replicationManager.setVersion(fileDelta.sequencingVersion());
            return failDelta;
        }
    }

    private final Map<String, FollowerDeltaApplier> followerExecutors = new ConcurrentHashMap<>();


}
