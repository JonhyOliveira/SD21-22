package tp1.impl.servers.common.replication;

import util.Json;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Versioning format used for replication
 */
public class Version implements Comparable<Version> {

    /**
     * Will be lower than any other version
     */
    public static final Version ZERO_VERSION = new Version(Long.MIN_VALUE, ""); // charAt = 0

    /**
     * Will be larger than any other version, make sure the first character of your
     * replicaID has a charAt value lower than "~"
     */
    public static final Version FUTURE_VERSION = new Version(Long.MAX_VALUE, "~"); // chartAt = 126

    private final AtomicLong v;
    private final AtomicReference<String> replicaID;

    public Version(AtomicLong v, AtomicReference<String> replicaID) {
        this.v = v;
        this.replicaID = replicaID;
    }

    public Version(Long v, String replicaID) {
        this(new AtomicLong(v), new AtomicReference<>(replicaID));
    }

    public Long v() {
        return v.get();
    }

    public String replicaID() {
        return replicaID.get();
    }

    @Override
    public int compareTo(Version o) {
        if (v.longValue() != o.v.longValue())
            return (int) (v.get() - o.v.get());
        else
            return replicaID.get().compareTo(o.replicaID.get());
    }

    @Override
    public String toString() {
        return Json.getInstance().toJson(this);
    }

    /**
     * Generates a successor to this version
     *
     * @param replicaID the replica to generate a successor for
     * @return the successor version
     */
    public synchronized Version next(String replicaID) {
        v.incrementAndGet();
        this.replicaID.set(replicaID);
        return this;
    }

    /**
     * Generates a successor to this version for the currently associated replica
     *
     * @return the successor replica
     */
    public Version next() {
        return this.next(this.replicaID.get());
    }

    public void set(Version o) {
        replicaID.set(o.replicaID.get());
        v.set(o.v.get());
    }

    public Version copy() {
        return new Version(v.get(), replicaID.get());
    }
}

