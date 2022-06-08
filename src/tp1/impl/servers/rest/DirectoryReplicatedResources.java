package tp1.impl.servers.rest;

import com.google.gson.Gson;
import jakarta.inject.Singleton;
import tp1.api.FileDelta;
import tp1.api.FileInfo;
import tp1.api.service.rest.RestDirectory;
import tp1.impl.servers.common.JavaDirectorySynchronizer;
import tp1.impl.servers.common.replication.ReplicationManager;
import tp1.impl.servers.common.replication.Version;
import util.Json;

import java.util.List;

@Singleton
public class DirectoryReplicatedResources extends DirectoryResources implements RestDirectory {


    final Gson json = Json.getInstance();
    final ReplicationManager replicationManager;


    public DirectoryReplicatedResources(String serviceURI) {
        super();
        var i = new JavaDirectorySynchronizer(serviceURI);
        this.impl = i;
        this.replicationManager = i.replicationManager();
    }

    @Override
    public FileInfo writeFile(String version, String filename, byte[] data, String userId, String password) {
        this.replicationManager.waitForVersion(json.fromJson(version, Version.class));
        return super.writeFile(version, filename, data, userId, password);
    }

    @Override
    public void deleteFile(String version, String filename, String userId, String password) {
        this.replicationManager.waitForVersion(json.fromJson(version, Version.class));
        super.deleteFile(version, filename, userId, password);
    }

    @Override
    public void shareFile(String version, String filename, String userId, String userIdShare, String password) {
        this.replicationManager.waitForVersion(json.fromJson(version, Version.class));
        super.shareFile(version, filename, userId, userIdShare, password);
    }

    @Override
    public void unshareFile(String version, String filename, String userId, String userIdShare, String password) {
        this.replicationManager.waitForVersion(json.fromJson(version, Version.class));
        super.unshareFile(version, filename, userId, userIdShare, password);
    }

    @Override
    public byte[] getFile(String version, String filename, String userId, String accUserId, String password) {
        this.replicationManager.waitForVersion(json.fromJson(version, Version.class));
        return super.getFile(version, filename, userId, accUserId, password);
    }

    @Override
    public List<FileInfo> lsFile(String version, String userId, String password) {
        this.replicationManager.waitForVersion(json.fromJson(version, Version.class));
        return super.lsFile(version, userId, password);
    }

    @Override
    public void deleteUserFiles(String version, String userId, String password, String token) {
        this.replicationManager.waitForVersion(json.fromJson(version, Version.class));
        super.deleteUserFiles(version, userId, password, token);
    }

    @Override
    public String getVersion(String token) {
        Log.info(String.format("REST getVersion: token = %s\n", token));
        return super.resultOrThrow(impl.getVersion(token));
    }

    @Override
    public void applyDelta(String version, String token, FileDelta delta) {
        Log.info(String.format("REST applyDelta: version = %s, delta = %s, token = %s\n", version, delta, token));
        super.resultOrThrow(impl.applyDelta(version, token, delta));
    }
}
