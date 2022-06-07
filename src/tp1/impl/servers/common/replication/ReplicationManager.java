package tp1.impl.servers.common.replication;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ReplicationManager {

    private static final String ELECTION_NAME = "directories";
    private static final Long DEFAULT_WAIT_TIME = 1000L;

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

    public boolean amLeader() {
        return election.amLeader();
    }

    public LeaderElection.Candidate leader() {
        return election.leader();
    }

    public List<LeaderElection.Candidate> followers() {
        return election.leader().others();
    }

    public String replicaID() {
        return election.whoAmI();
    }

    public void waitForVersion(Version n) {
        this.waitForVersion(n, DEFAULT_WAIT_TIME);
    }

    public void waitForVersion(Version n, Long waitTime) {
        synchronized (this) {
            while (version().compareTo(n) < 0) {
                try {
                    this.wait(waitTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void setVersion(Version n) {
        synchronized (this) {
            this.version.set(n);
            this.notifyAll();
        }
    }
}
