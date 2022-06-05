package tp1.impl.servers.rest;

import com.google.gson.Gson;
import jakarta.inject.Singleton;
import org.apache.kafka.common.utils.Java;
import tp1.api.FileInfo;
import tp1.impl.clients.soap.SoapDirectoryClient;
import tp1.impl.servers.common.JavaDirectorySynchronizer;
import tp1.impl.servers.common.replication.DirectoryOperation;
import tp1.impl.servers.common.replication.Version;
import util.Json;
import util.kafka.KafkaSubscriber;

import java.util.List;
import java.util.logging.Logger;

@Singleton
public class DirectoryReplicatedResources extends DirectoryResources {

    private static final Logger Log = Logger.getLogger(DirectoryReplicatedResources.class.getName());
    private static final String ELECTION_NAME = "directoryLeader";

    final Gson json = Json.getInstance();

    public DirectoryReplicatedResources(JavaDirectorySynchronizer synchronizer) {
        super();
        impl = synchronizer;
        KafkaSubscriber.createSubscriber("kafka:9092", List.of(DirectoryOperation.NAMESPACE), "earliest")
                .start(false, ((JavaDirectorySynchronizer) impl));
    }

    @Override
    public FileInfo writeFile(String version, String filename, byte[] data, String userId, String password) {
        ((JavaDirectorySynchronizer) impl).waitForVersion(json.fromJson(version, Version.class));
        return super.writeFile(version, filename, data, userId, password);
    }

    @Override
    public byte[] getFile(String version, String filename, String userId, String accUserId, String password) {
        ((JavaDirectorySynchronizer) impl).waitForVersion(json.fromJson(version, Version.class));
        return super.getFile(version, filename, userId, accUserId, password);
    }

    @Override
    public void deleteFile(String version, String filename, String userId, String password) {
        ((JavaDirectorySynchronizer) impl).waitForVersion(json.fromJson(version, Version.class));
        super.deleteFile(version, filename, userId, password);
    }

    @Override
    public void shareFile(String version, String filename, String userId, String userIdShare, String password) {
        ((JavaDirectorySynchronizer) impl).waitForVersion(json.fromJson(version, Version.class));
        super.shareFile(version, filename, userId, userIdShare, password);
    }

    @Override
    public void unshareFile(String version, String filename, String userId, String userIdShare, String password) {
        ((JavaDirectorySynchronizer) impl).waitForVersion(json.fromJson(version, Version.class));
        super.unshareFile(version, filename, userId, userIdShare, password);
    }

    @Override
    public List<FileInfo> lsFile(String version, String userId, String password) {
        ((JavaDirectorySynchronizer) impl).waitForVersion(json.fromJson(version, Version.class));
        return super.lsFile(version, userId, password);
    }

    @Override
    public void deleteUserFiles(String version, String userId, String password, String token) {
        ((JavaDirectorySynchronizer) impl).waitForVersion(json.fromJson(version, Version.class));
        super.deleteUserFiles(version, userId, password, token);
    }
}
