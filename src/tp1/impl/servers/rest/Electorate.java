package tp1.impl.servers.rest;

import org.apache.zookeeper.AddWatchMode;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import util.zookeeper.Zookeeper;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;


public class Electorate {

    private static final Logger Log = Logger.getLogger(Electorate.class.getName());

    private final AtomicBoolean elected;

    /**
     * Conducts an election using Zookeeper.
     * Leaders are chosen based on the oldest node.
     */
    public Electorate(String electionName) {
        this.elected = new AtomicBoolean(false);

            Zookeeper zookeeper = Zookeeper.getInstance();

            zookeeper.createNode("/Election.%s".formatted(electionName), new byte[0], CreateMode.PERSISTENT);
            var myName = zookeeper.createNode("/Election-%s/candidate", new byte[0], CreateMode.EPHEMERAL_SEQUENTIAL);

            var s = zookeeper.getChildren("/Election-%s")
                    .stream().sorted()
                    .filter(electionCandidate -> myName.compareTo(electionCandidate) < 0); // filter candidates before me

            // get the candidate right before me
            s.skip(Math.max(s.count() - 1, 0)).findFirst().ifPresentOrElse(imNext -> {
                try {
                    zookeeper.client().addWatch(imNext, e -> { // wait for imNext to fail
                        if (e.getType().equals(Watcher.Event.EventType.NodeDeleted)) {
                            this.promoteToLeader();
                        }
                    }, AddWatchMode.PERSISTENT);
                } catch (KeeperException | InterruptedException e) {
                    Log.severe("Error with watch :(");
                    e.printStackTrace();
                }
            }, this::promoteToLeader);

    }

    /**
     * Whether or I have been elected
     * @return true if elected, false otherwise
     */
    public boolean amElected() {
        synchronized (elected) {
            return elected.get();
        }
    }

    /**
     * Waits until I'm elected
     */
    public void waitUntilElected() {
        while (!this.amElected()) {
            try {
                this.wait(Integer.MAX_VALUE);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Promote myself to leader
     */
    protected void promoteToLeader() {
        synchronized (elected) {
            if (!this.elected.get()) {
                this.elected.set(true);
                this.notifyAll();
            }
        }
    }

}
