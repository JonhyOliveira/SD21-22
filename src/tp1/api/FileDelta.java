package tp1.api;

import tp1.impl.servers.common.JavaFiles;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.regex.Pattern;

public class FileDelta implements Serializable {

    private String userId, filename;
    private boolean removed;
    private Set<String> addedShares, removedShares, addedURIs, removedURIs;

    public FileDelta() {
        this.addedShares = ConcurrentHashMap.newKeySet();
        this.removedShares = ConcurrentHashMap.newKeySet();
        this.addedURIs = ConcurrentHashMap.newKeySet();
        this.removedURIs = ConcurrentHashMap.newKeySet();
    }

    public FileDelta(String userId, String filename) {
        this(userId, filename, true, null, null, null, null);
    }

    public FileDelta(String userId, String filename, boolean removed, Set<String> addedURIs, Set<String> removedURIs,
                     Set<String> addedShares, Set<String> removedShares) {
        this.userId = userId;
        this.filename = filename;
        this.removed = removed;
        this.addedURIs = addedURIs == null ? ConcurrentHashMap.newKeySet() : addedURIs;
        this.removedURIs = removedURIs == null ? ConcurrentHashMap.newKeySet() : removedURIs;
        this.addedShares = addedShares == null ? ConcurrentHashMap.newKeySet() : addedShares;
        this.removedShares = removedShares == null ? ConcurrentHashMap.newKeySet() : removedShares;
    }

    public String getUserId() {
        return userId;
    }

    public String getFilename() {
        return filename;
    }

    public boolean isRemoved() {
        return removed;
    }

    public Set<String> getAddedURIs() {
        return addedURIs;
    }

    public Set<String> getRemovedURIs() {
        return removedURIs;
    }

    public Set<String> getAddedShares() {
        return addedShares;
    }

    public Set<String> getRemovedShares() {
        return removedShares;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setRemoved(boolean removed) {
        this.removed = removed;
    }

    public void setAddedShares(Set<String> addedShares) {
        this.addedShares = addedShares;
    }

    public void setRemovedShares(Set<String> removedShares) {
        this.removedShares = removedShares;
    }

    public void setAddedURIs(Set<String> addedURIs) {
        this.addedURIs = addedURIs;
    }

    public void setRemovedURIs(Set<String> removedURIs) {
        this.removedURIs = removedURIs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FileDelta fileDelta = (FileDelta) o;

        if (!userId.equals(fileDelta.userId)) return false;
        return filename.equals(fileDelta.filename);
    }

    @Override
    public int hashCode() {
        int result = userId.hashCode();
        result = 31 * result + filename.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "FileDelta{" +
                "userId='" + userId + '\'' +
                ", filename='" + filename + '\'' +
                ", removed=" + removed +
                ", addedURIs=" + addedURIs +
                ", removedURIs=" + removedURIs +
                ", addedShares=" + addedShares +
                ", removedShares=" + removedShares +
                '}';
    }
}
