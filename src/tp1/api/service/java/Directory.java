package tp1.api.service.java;

import tp1.api.FileInfo;
import tp1.impl.servers.common.JavaDirectoryState;
import tp1.impl.servers.common.replication.Version;

import java.util.List;

public interface Directory {

    String SERVICE_NAME = "directory";

    Result<FileInfo> writeFile(String filename, byte[] data, String userId, String password);

    Result<Void> deleteFile(String filename, String userId, String password);

    Result<Void> shareFile(String filename, String userId, String userIdShare, String password);

    Result<Void> unshareFile(String filename, String userId, String userIdShare, String password);

    Result<byte[]> getFile(String filename, String userId, String accUserId, String password);

    Result<List<FileInfo>> lsFile(String userId, String password);

    Result<Void> deleteUserFiles(String userId, String password, String token);

    Result<Version> getVersion(String token);

    Result<Void> applyDelta(JavaDirectoryState.FileDelta delta, String token);
}
