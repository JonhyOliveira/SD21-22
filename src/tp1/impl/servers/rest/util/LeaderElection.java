package tp1.impl.servers.rest.util;

import org.apache.zookeeper.AddWatchMode;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import util.zookeeper.Zookeeper;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.stream.Collectors;


public class LeaderElection {

    private static final Logger Log = Logger.getLogger(LeaderElection.class.getName());
    public static final String ELECTION_NAME_FORMAT = "/Election.%s";
    public static final String ELECTION_CANDIDATE_FORMAT = "/Election-%s/candidate";

    private final AtomicReference<Node> leader;
    private final Zookeeper zookeeper;
    private final String myName;
    private final String electionName;

    /**
     * Conducts an election using Zookeeper.
     * Leaders are chosen based on the oldest node.
     */
    public LeaderElection(String identifier, String electionName) {
        leader = new AtomicReference<>(null);
        zookeeper = Zookeeper.getInstance();

        this.electionName = zookeeper.createNode(ELECTION_NAME_FORMAT.formatted(electionName), new byte[0], CreateMode.PERSISTENT);
        myName = zookeeper.createNode(ELECTION_CANDIDATE_FORMAT, identifier.getBytes(StandardCharsets.UTF_8), CreateMode.EPHEMERAL_SEQUENTIAL);

        watchOutForElection();
    }

    private void watchOutForElection() {
        zookeeper.getChildren(ELECTION_NAME_FORMAT.formatted(electionName))
                .stream()
                .sorted()
                .findFirst()
                .ifPresentOrElse(l -> {
                    // do election
                    try {
                        promoteToLeader(Node.fromZK(l, zookeeper));
                    } catch (InterruptedException | KeeperException e) {
                        e.printStackTrace();
                    }

                    if (amElected())
                        return;

                    // set up watch
                    try {
                        zookeeper.client().addWatch(l, e -> { // wait for leader to fail
                            if (e.getType().equals(Watcher.Event.EventType.NodeDeleted)) {
                                watchOutForElection();
                            }
                        }, AddWatchMode.PERSISTENT);
                    } catch (KeeperException | InterruptedException e) {
                        Log.severe("Error with watch :(");
                        e.printStackTrace();
                    }
                }, () -> {
                    // this should never happen
                    Log.severe("Am I alone?");
                    throw new IllegalStateException();
                });
    }

    public Node leader() {
        return leader.get();
    }

    /**
     * Whether or I have been elected
     * @return true if elected, false otherwise
     */
    public boolean amElected() {
        return leader().name().equals(myName);
    }

    /**
     * Waits until I'm elected
     */
    public void waitUntilElected() {
        synchronized (leader) {
            while (!this.amElected()) {
                try {
                    leader.wait(Integer.MAX_VALUE);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void promoteToLeader(Node node) {
        synchronized (leader) {
            this.leader.set(node);
            leader.notifyAll();
        }
    }

    /**
     * Gets the list of followers if I am the leader
     * @return the list of followers
     * @throws IllegalStateException if I am not the leader
     */
    public List<Node> followers() throws IllegalStateException {
        synchronized (leader) {
            if (amElected())
                return zookeeper.getChildren(electionName).stream()
                        .filter(followerName -> !followerName.equals(myName)) // filter myself out
                        .map(followerName -> {
                            try {
                                return new Node(followerName,
                                        new String(zookeeper.client().getData(followerName, false, null), StandardCharsets.UTF_8));
                            } catch (KeeperException | InterruptedException e) {
                                e.printStackTrace();
                            }

                            return null;
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

            else
                throw new IllegalStateException();
        }
    }

    public record Node(String name, String serviceURI) {
        public static Node fromZK(String nodePath, Zookeeper zk) throws InterruptedException, KeeperException {
            return new Node(nodePath, new String(zk.client().getData(nodePath, false, null), StandardCharsets.UTF_8));
        }
    }

}
