package tp1.impl.servers.common.replication;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import util.zookeeper.Zookeeper;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;


public class LeaderElection implements Watcher {

    private static final Logger Log = Logger.getLogger(LeaderElection.class.getName());
    private static final String ELECTION_PATH_FORMAT = "/Election.%s";
    private static final String ELECTION_CANDIDATE_FORMAT = "/Election.%s/candidate";
    private static final int CANDIDATE_DISCOVERY_SLEEP_TIME = 100;

    private final AtomicReference<Candidate> me;
    private final Zookeeper zookeeper;
    private final String myName;
    private final String electionName;
    private final SortedMap<String, Candidate> candidates = new ConcurrentSkipListMap<>();

    private final Config config;

    public record Config(Consumer<Candidate> onNewCandidate, Consumer<Candidate> onCandidateGone) {}

    /**
     * Conducts an election using Zookeeper.
     * Leaders are chosen based on the oldest node.
     */
    public LeaderElection(String identifier, String electionName, Config config) {
        this.config = config;
        this.electionName = electionName;
        me = new AtomicReference<>(null);
        zookeeper = Zookeeper.getInstance();

        Log.info("Created election %s"
                .formatted(zookeeper.createNode(ELECTION_PATH_FORMAT.formatted(electionName), new byte[0], CreateMode.PERSISTENT)));
        myName = zookeeper.createNode(ELECTION_CANDIDATE_FORMAT.formatted(electionName), identifier.getBytes(StandardCharsets.UTF_8), CreateMode.EPHEMERAL_SEQUENTIAL);
        Log.info("My name is %s. My identifier is: %s".formatted(myName, identifier));
    }

    public static String candidatePath(String electionName, String candidateName) {
        return "%s/%s".formatted(ELECTION_PATH_FORMAT.formatted(electionName), candidateName);
    }

    /**
     * @return the leader of this instance
     */
    public Candidate leader() {
        return candidates.get(candidates.firstKey());
    }

    public String whoAmI() {
        return myName;
    }

    public boolean amLeader() {
        return me.get().equals(leader());
    }

    public void watchCandidates() {
        populateCandidates(this);
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        Log.info("Zookeeper Event [type = %s, path = %s]".formatted(watchedEvent.getType(), watchedEvent.getPath()));
        if (watchedEvent.getType().equals(Event.EventType.NodeChildrenChanged))
            populateCandidates(this);
        else
            zookeeper.getChildren(ELECTION_PATH_FORMAT.formatted(electionName), this);
    }

    private void populateCandidates(Watcher watcher) {

        var oldCandidatesZkPath = Set.copyOf(candidates.values().stream().map(Candidate::zkNodePath).collect(Collectors.toList()));
        Log.info("Old zkNodes: %s".formatted(oldCandidatesZkPath));

        var currCandidatesZkPath = zookeeper.getChildren(ELECTION_PATH_FORMAT.formatted(electionName), watcher)
                .stream()
                .map(s -> candidatePath(electionName, s))
                .collect(Collectors.toSet());

        Log.info("Current zkNodes: %s".formatted(currCandidatesZkPath));

        var newCandidates = currCandidatesZkPath.stream()
                .filter(s -> !this.candidates.containsKey(s)) // only candidates that haven't been created
                .map(s -> {
                    try {
                        return Candidate.fromZK(s, zookeeper, this.candidates);
                    } catch (InterruptedException | KeeperException e) {
                        e.printStackTrace();
                    }
                    return null;
                }).filter(Objects::nonNull)
                .collect(Collectors.toSet());

        var removedCandidates = this.candidates.values().stream()
                .filter(candidate -> !currCandidatesZkPath.contains(candidate.zkNodePath()))
                .collect(Collectors.toSet());

        // update structures
        newCandidates.forEach(candidate -> {
                    this.candidates.put(candidate.zkNodePath, candidate); // update entries
                    if (myName.equals(candidate.zkNodePath)) // find myself in the updated entries
                        me.set(candidate);
                });

        removedCandidates.forEach(candidate -> {
                    this.candidates.remove(candidate.serviceURI().toString());
                });

        // notify
        newCandidates.forEach(config.onNewCandidate);
        removedCandidates.forEach(config.onCandidateGone);
    }

    /*
    public static class Follower extends Candidate {

        public Follower(String zkNodePath, URI serviceURI, SortedMap<String, Candidate> candidates) {
            super(zkNodePath, serviceURI, candidates);
        }

        @Override
        public boolean isLeader() {
            return false;
        }

        @Override
        public Candidate leader() {
            return super.leader();
        }

    }

    public static class Leader extends Candidate {

        public Leader(String zkNodePath, URI serviceURI, SortedMap<String, Candidate> candidates) {
            super(zkNodePath, serviceURI, candidates);
        }

        @Override
        public boolean isLeader() {
            return true;
        }

        @Override
        public Leader leader() {
            return this;
        }

    }
    */

    public record Candidate(String zkNodePath, URI serviceURI, SortedMap<String, Candidate> candidates, AtomicBoolean isAlive) implements Comparable<Candidate> {

        public static Candidate fromZK(String nodePath, Zookeeper zk, SortedMap<String, Candidate> candidates) throws InterruptedException, KeeperException {
            Log.finest("Parsing %s from zookeeper".formatted(nodePath));
            var data = zk.client().getData(nodePath, false, null);
            var serviceURI = URI.create(new String(data, StandardCharsets.UTF_8));

            return new Candidate(nodePath, serviceURI, candidates, new AtomicBoolean(true));
                /*
                if (candidates.first().zkNodePath.equals(nodePath))
                    return new Leader(nodePath, serviceURI, candidates);
                else
                    return new Follower(nodePath, serviceURI, candidates);*/
        }

        public boolean isLeader() {
            return candidates.get(candidates.firstKey()).equals(this);
        }

        public boolean isFollower() {
            return !isLeader();
        }

        public List<Candidate> others() {
            return candidates.values().stream().filter(candidate -> !this.equals(candidate)).collect(Collectors.toList());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Candidate candidate = (Candidate) o;

            return serviceURI.equals(candidate.serviceURI);
        }

        @Override
        public int hashCode() {
            return serviceURI.hashCode();
        }

        @Override
        public int compareTo(Candidate o) {
            return this.zkNodePath.compareTo(o.zkNodePath);
        }

        @Override
        public String toString() {
            return "Candidate{" +
                    "zkNodePath='" + zkNodePath + '\'' +
                    ", serviceURI=" + serviceURI +
                    ", candidates=" + candidates.values().stream().map(Candidate::zkNodePath) +
                    '}';
        }
    }

}
