package tp1.impl.servers.common.replication;

import tp1.impl.clients.Clients;
import util.Token;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ReplicationManager {

    private static final String ELECTION_NAME = "directories";
    private static final Long DEFAULT_WAIT_TIME = 1000L;
    private static final Long MAX_WAIT = 10L;

    private final AtomicReference<Version> version;

    public final LeaderElection election;

    public ReplicationManager(String identifier, LeaderElection.Config electionConfig) {
        election = new LeaderElection(identifier, ELECTION_NAME, electionConfig);
        version = new AtomicReference<>(Version.ZERO_VERSION);
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
        if (version().compareTo(n) < 0) {
            long startTime = System.currentTimeMillis();
            synchronized (this) {
                while (version().compareTo(n) < 0 && System.currentTimeMillis() - startTime < MAX_WAIT * 1000) {
                    try {
                        System.err.println("Waiting for version %s, currently on version %s".formatted(n, version()));
                        this.wait(waitTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            System.err.println("Done waiting");
        }
    }

    public void waitToBeLeader() {
        while (!amLeader()) {
            try {
                Thread.sleep(DEFAULT_WAIT_TIME);
            } catch (InterruptedException e) {
                e.printStackTrace();
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
