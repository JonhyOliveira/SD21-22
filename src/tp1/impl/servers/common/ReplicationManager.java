package tp1.impl.servers.common;

import org.apache.zookeeper.CreateMode;
import tp1.impl.servers.common.replication.DirectoryOperation;
import tp1.impl.servers.common.replication.Version;
import util.kafka.KafkaPublisher;
import util.zookeeper.Zookeeper;

import java.util.logging.Logger;

public class ReplicationManager {

    private final static Logger Log = Logger.getLogger(ReplicationManager.class.getName());
    private static final String PARENT_NAME = "dirs";

    private final KafkaPublisher publisher = KafkaPublisher.createPublisher("kafka:9092");
    private final String replicaId;
    private final Version currentVersion;

    /**
     * Creates a new replication manager
     */
    public ReplicationManager() {
        var zookeeper = Zookeeper.getInstance();
        zookeeper.createNode("/%s".formatted(PARENT_NAME), new byte[0], CreateMode.PERSISTENT);
        replicaId = zookeeper.createNode("/%s/dir_".formatted(PARENT_NAME), new byte[0], CreateMode.EPHEMERAL_SEQUENTIAL);
        currentVersion = new Version(0L, replicaId);
    }

    /**
     * The current version of the replicaID
     *
     * @return the current version of the replicaID
     */
    public Version getCurrentVersion() {
        return currentVersion;
    }

    public void waitForVersion(Version v) {
        this.waitForVersion(v, Long.MAX_VALUE);
    }

    public void waitForVersion(Version v, long waitPeriod) {
        while (currentVersion.compareTo(v) < 0) {
            try {
                this.wait(waitPeriod);
            } catch (InterruptedException e) {
                Log.info("Exception while waiting for version");
                e.printStackTrace();
            }
        }
    }

    public void send(DirectoryOperation.OperationType operationType, String data) {
        synchronized (currentVersion) {
            this.currentVersion.next(this.replicaId);
            var operation = operationType.toOperation(this.currentVersion, data);
            publisher.publish(operation.toRecord());
        }
    }

    /*private class Electorate {

        private final String myName;
        private final CountDownLatch meLeader = new CountDownLatch(1);

        public Electorate(String electionName) {
            var zookeeper = Zookeeper.getInstance();
            zookeeper.createNode("/Election.%s".formatted(electionName), new byte[0], CreateMode.CONTAINER); // will schedule for deletion on zookeeper if no children.
            myName = zookeeper.createNode("/Election.%s/c_".formatted(electionName), new byte[0], CreateMode.EPHEMERAL_SEQUENTIAL);
        }

        public String whoAmI() {
            return myName;
        }

        public void waitUntilElected() {
            try {
                this.meLeader.await();
            } catch (InterruptedException e) {
                Log.warning("Killed while waiting to be elected");
            }
        }

        private void IAmLeaderNow() {
            this.meLeader.countDown();
        }
    }*/
}
