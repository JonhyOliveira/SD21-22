package tp1.impl.servers.common.replication;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import util.zookeeper.Zookeeper;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.stream.Collectors;


public class LeaderElection {

    private static final Logger Log = Logger.getLogger(LeaderElection.class.getName());
    private static final String ELECTION_PATH_FORMAT = "/Election.%s";
    private static final String ELECTION_CANDIDATE_FORMAT = "/Election.%s/candidate";
    private static final int CANDIDATE_DISCOVERY_SLEEP_TIME = 100;

    private final AtomicReference<Candidate> me;
    private final Zookeeper zookeeper;
    private final String myName;
    private final String electionName;
    private final SortedMap<String, Candidate> candidates = new ConcurrentSkipListMap<>();

    /**
     * Conducts an election using Zookeeper.
     * Leaders are chosen based on the oldest node.
     */
    public LeaderElection(String identifier, String electionName) {
        this.electionName = electionName;
        me = new AtomicReference<>(null);
        zookeeper = Zookeeper.getInstance();

        Log.info("Created election %s"
                .formatted(zookeeper.createNode(ELECTION_PATH_FORMAT.formatted(electionName), new byte[0], CreateMode.PERSISTENT)));
        myName = zookeeper.createNode(ELECTION_CANDIDATE_FORMAT.formatted(electionName), identifier.getBytes(StandardCharsets.UTF_8), CreateMode.EPHEMERAL_SEQUENTIAL);
        Log.info("My name is %s. My identifier is: %s".formatted(myName, identifier));


        watchOutForElection();
    }

    private void watchOutForElection() {
        Log.info("Looking out for an election...");

        new Thread(() -> {
            for (;;) {
                List<String> zkCandidates = Collections.emptyList();
                try {
                    zkCandidates = zookeeper.getChildren(ELECTION_PATH_FORMAT.formatted(electionName));
                } catch (KeeperException e) {
                    if (e.code().equals(KeeperException.Code.CONNECTIONLOSS)) {
                        return;
                    }
                }

                for (String zkCandidate : zkCandidates) {
                    var path = candidatePath(electionName, zkCandidate);
                    try {
                        var can = Candidate.fromZK(path, zookeeper, candidates);
                        if (myName.equals(path))
                            me.set(can);
                        candidates.put(zkCandidate, can);
                    } catch (InterruptedException | KeeperException e) {
                        e.printStackTrace();
                    }
                }

                try {
                    Thread.sleep(CANDIDATE_DISCOVERY_SLEEP_TIME);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }

        }).start();
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

    public record Candidate(String zkNodePath, URI serviceURI,
                            SortedMap<String, Candidate> candidates) implements Comparable<Candidate> {

        public static Candidate fromZK(String nodePath, Zookeeper zk, SortedMap<String, Candidate> candidates) throws InterruptedException, KeeperException {
            Log.finest("Parsing %s from zookeeper".formatted(nodePath));
            var data = zk.client().getData(nodePath, false, null);
            var serviceURI = URI.create(new String(data, StandardCharsets.UTF_8));

            return new Candidate(nodePath, serviceURI, candidates);
                /*
                if (candidates.first().zkNodePath.equals(nodePath))
                    return new Leader(nodePath, serviceURI, candidates);
                else
                    return new Follower(nodePath, serviceURI, candidates);*/
        }


        public List<Candidate> others() {
            return candidates.values().stream().filter(candidate -> !this.equals(candidate)).collect(Collectors.toList());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Candidate candidate = (Candidate) o;

            return zkNodePath.equals(candidate.zkNodePath);
        }

        @Override
        public int hashCode() {
            return zkNodePath.hashCode();
        }

        @Override
        public int compareTo(Candidate o) {
            return this.zkNodePath.compareTo(o.zkNodePath);
        }


    }

}
