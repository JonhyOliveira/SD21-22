package tp1.impl.servers.common.replication;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import util.zookeeper.Zookeeper;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.stream.Collectors;


public class LeaderElection {

    private static final Logger Log = Logger.getLogger(LeaderElection.class.getName());
    private static final String ELECTION_PATH_FORMAT = "/Election.%s";
    private static final String ELECTION_CANDIDATE_FORMAT = "/Election.%s/candidate";

    private final AtomicReference<Node> leader;
    private final Zookeeper zookeeper;
    private final String myName;
    private final String electionName;

    /**
     * Conducts an election using Zookeeper.
     * Leaders are chosen based on the oldest node.
     */
    public LeaderElection(String identifier, String electionName) {
        this.electionName = electionName;
        leader = new AtomicReference<>(null);
        zookeeper = Zookeeper.getInstance();

        Log.info("Created election %s"
                .formatted(zookeeper.createNode(ELECTION_PATH_FORMAT.formatted(electionName), new byte[0], CreateMode.PERSISTENT)));
        myName = zookeeper.createNode(ELECTION_CANDIDATE_FORMAT.formatted(electionName), identifier.getBytes(StandardCharsets.UTF_8), CreateMode.EPHEMERAL_SEQUENTIAL);
        Log.info("My name is %s. My identifier is: %s".formatted(myName, identifier));


        watchOutForElection();
    }

    private void watchOutForElection() {
        Log.info("Looking out for an election...");

        zookeeper.getChildren(ELECTION_PATH_FORMAT.formatted(electionName), e -> {
                    if (e.getType().equals(Watcher.Event.EventType.NodeDeleted))
                        Log.info(e.getPath());
                })
                .stream()
                .sorted()
                .findFirst()
                .ifPresentOrElse(l -> {
                    // elect
                    try {
                        promoteToLeader(Node.fromZK(candidatePath(l), zookeeper));
                    } catch (InterruptedException | KeeperException | URISyntaxException e) {
                        e.printStackTrace();
                    }
                }, () -> {
                    // this should never happen
                    throw new IllegalStateException("Am I alone?");
                });
    }

    public String candidatePath(String candidateName) {
        return "%s/%s".formatted(ELECTION_PATH_FORMAT.formatted(electionName), candidateName);
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
            if (node.name().equals(myName))
                Log.info("I am the law");
            else
                Log.info("%s is the leader now. Service URI: %s".formatted(node.name(), node.serviceURI()));
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
                return zookeeper.getChildren(ELECTION_PATH_FORMAT.formatted(electionName)).stream()
                        .filter(followerName -> !followerName.equals(myName)) // filter myself out
                        .map(followerName -> {
                            try {
                                return Node.fromZK(candidatePath(followerName), zookeeper);
                            } catch (KeeperException | InterruptedException | URISyntaxException e) {
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

    public record Node(String name, URI serviceURI) {
        public static Node fromZK(String nodePath, Zookeeper zk) throws InterruptedException, KeeperException, URISyntaxException {
            var data = zk.client().getData(nodePath, false, null);
            var serviceURI = new URI(new String(data, StandardCharsets.UTF_8));

            return new Node(nodePath, serviceURI);
        }
    }

}
