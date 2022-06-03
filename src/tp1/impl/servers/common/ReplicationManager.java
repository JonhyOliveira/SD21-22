package tp1.impl.servers.common;

import java.util.concurrent.atomic.AtomicLong;

public class ReplicationManager {

    private static ReplicationManager instance;

    public static ReplicationManager getInstance() {
        if (instance == null) {
            instance = new ReplicationManager();
        }
        return instance;
    }

    AtomicLong currentVersion;

    public ReplicationManager() {
        this.currentVersion = new AtomicLong(0);
    }



    public Long getCurrentVersion() {
        return this.currentVersion.get();
    }
}
