package tp1.impl.servers.common.replication;

import util.Json;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Versioning format used for replication and operation serialization
 */
public class Version implements Comparable<Version> {

    /**
     * Will be lower than any other version
     */
    public static final Version ZERO_VERSION = new Version(0L, ""); // charAt = 0

    /**
     * Will be larger than any other version, make sure the first character of your
     * replicaID has a charAt value lower than "~"
     */
    public static final Version FUTURE_VERSION = new Version(Long.MAX_VALUE, "~"); // chartAt = 126

    private AtomicLong v;
    private AtomicReference<String> replicaID;

    public Version(AtomicLong v, AtomicReference<String> replicaID) {
        this.v = v;
        this.replicaID = replicaID;
    }

    public Version(Long v, String replicaID) {
        this(new AtomicLong(v), new AtomicReference<>(replicaID));
    }

    public Long getVersion() {
        return v.get();
    }

    public String getReplicaID() {
        return replicaID.get();
    }

    public void setVersion(Long v) {
        if (this.v == null)
            this.v = new AtomicLong(v);
        else
            this.v.set(v);
    }

    public void setReplicaID(AtomicReference<String> replicaID) {
        this.replicaID = replicaID;
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
     * Generates a direct successor to this version
     *
     * @param replicaID the replica to generate a successor for
     * @return a successor version
     */
    public synchronized Version next(String replicaID) {
        v.incrementAndGet();
        this.replicaID.set(replicaID);
        return this;
    }

    /**
     * Generates a direct successor to this version for the currently associated replica
     *
     * @return a successor version
     */
    public Version next() {
        return this.next(this.replicaID.get());
    }

    public Version nextCopy(String replicaID) {
        return this.copy().next(replicaID);
    }

    /**
     * Generates a copy of a direct successor of this version the currently associated
     *
     * @return
     */
    public Version nextCopy() {
        return this.nextCopy(this.replicaID.get());
    }

    /**
     * Generates a directly previous version to this version (version number - 1)
     * (only for comparative purposes)
     *
     * @return a previous version
     */
    public synchronized Version previous() {
        return new Version(v.get()-1, "");
    }

    public void setVersion(Version o) {
        replicaID.set(o.replicaID.get());
        v.set(o.v.get());
    }

    public Version copy() {
        return new Version(v.get(), replicaID.get());
    }
}

