package tp1.impl.servers.common.replication;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class ReplicationManager {

    private static final String ELECTION_NAME = "directories";

    private final AtomicReference<Version> version = new AtomicReference<>(null);

    public final LeaderElection election;

    public ReplicationManager(String identifier) {
        election = new LeaderElection(identifier, ELECTION_NAME);
    }

    public Version version() {
        return version.get();
    }

    public LeaderElection election() {
        return election;
    }

    public synchronized void waitForVersion(Version n, Long waitTime) {
        while (version().compareTo(n) < 0) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void setVersion(Version n) {
        this.version.set(n);
        this.notifyAll();
    }
}
