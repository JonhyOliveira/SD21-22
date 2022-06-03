package tp1.impl.servers.rest;

import jakarta.inject.Singleton;
import tp1.api.FileInfo;
import tp1.api.service.java.Directory;
import tp1.impl.servers.common.JavaDirectoryReplicated;
import tp1.impl.servers.common.ReplicationManager;

import java.util.List;
import java.util.logging.Logger;

@Singleton
public class DirectoryReplicatedResources extends DirectoryResources {

    private static final Logger Log = Logger.getLogger(DirectoryReplicatedResources.class.getName());

    final Directory impl;
    final ReplicationManager replicationManager;

    public DirectoryReplicatedResources(ReplicationManager replicationManager) {
        super();
        this.replicationManager = replicationManager;

        impl = new JavaDirectoryReplicated();
    }


    @Override
    public FileInfo writeFile(Long version, String filename, byte[] data, String userId, String password) {
        return null;
    }

    @Override
    public void deleteFile(Long version, String filename, String userId, String password) {

    }

    @Override
    public void shareFile(Long version, String filename, String userId, String userIdShare, String password) {

    }

    @Override
    public void unshareFile(Long version, String filename, String userId, String userIdShare, String password) {

    }

    @Override
    public byte[] getFile(Long version, String filename, String userId, String accUserId, String password) {
        return null;
    }

    @Override
    public void deleteUserFiles(Long version, String userId, String password, String token) {

    }
}
