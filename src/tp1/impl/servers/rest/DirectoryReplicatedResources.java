package tp1.impl.servers.rest;

import jakarta.inject.Singleton;
import tp1.api.FileInfo;
import tp1.api.service.java.Result;
import tp1.api.service.rest.RestDirectory;
import tp1.impl.servers.common.JavaDirectory;
import tp1.impl.servers.common.JavaDirectoryState;
import tp1.impl.servers.common.JavaDirectorySynchronizer;
import tp1.impl.servers.common.replication.ReplicationManager;
import tp1.impl.servers.common.replication.Version;

import java.util.List;
import java.util.logging.Logger;

import static tp1.impl.clients.Clients.FilesClients;

@Singleton
public class DirectoryReplicatedResources extends DirectoryResources implements RestDirectory {

    /*
    final Gson json = Json.getInstance();
    final ReplicationManager replicationManager;
    */

    public DirectoryReplicatedResources(ReplicationManager replicationManager) {
        super();
        this.impl = new JavaDirectorySynchronizer(replicationManager);
        /* this.replicationManager = replicationManager; */
    }

    @Override
    public FileInfo writeFile(Long version, String filename, byte[] data, String userId, String password) {
        return super.writeFile(version, filename, data, userId, password);
    }

    @Override
    public void deleteFile(Long version, String filename, String userId, String password) {
        super.deleteFile(version, filename, userId, password);
    }

    @Override
    public void shareFile(Long version, String filename, String userId, String userIdShare, String password) {
        super.shareFile(version, filename, userId, userIdShare, password);
    }

    @Override
    public void unshareFile(Long version, String filename, String userId, String userIdShare, String password) {
        super.unshareFile(version, filename, userId, userIdShare, password);
    }

    @Override
    public byte[] getFile(Long version, String filename, String userId, String accUserId, String password) {
        return super.getFile(version, filename, userId, accUserId, password);
    }

    @Override
    public List<FileInfo> lsFile(Long version, String userId, String password) {
        return super.lsFile(version, userId, password);
    }

    @Override
    public void deleteUserFiles(Long version, String userId, String password, String token) {
        super.deleteUserFiles(version, userId, password, token);
    }

    @Override
    public Version getVersion(String token) {
        return null;
    }

    @Override
    public void applyDelta(JavaDirectoryState.FileDelta delta, String token) {

    }
}
