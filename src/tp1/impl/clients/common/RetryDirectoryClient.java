package tp1.impl.clients.common;

import tp1.api.FileInfo;
import tp1.api.service.java.Directory;
import tp1.api.service.java.Result;
import tp1.impl.servers.common.JavaDirectoryState;
import tp1.impl.servers.common.replication.Version;

import java.util.List;

public class RetryDirectoryClient extends RetryClient implements Directory {

    final Directory impl;

    public RetryDirectoryClient(Directory impl) {
        this.impl = impl;
    }

    @Override
    public Result<FileInfo> writeFile(String filename, byte[] data, String userId, String password) {
        return super.reTry(() -> impl.writeFile(filename, data, userId, password));
    }

    @Override
    public Result<Void> deleteFile(String filename, String userId, String password) {
        return super.reTry(() -> impl.deleteFile(filename, userId, password));

    }

    @Override
    public Result<Void> shareFile(String filename, String userId, String userIdShare, String password) {
        return super.reTry(() -> impl.shareFile(filename, userId, userIdShare, password));
    }

    @Override
    public Result<Void> unshareFile(String filename, String userId, String userIdShare, String password) {
        return super.reTry(() -> impl.unshareFile(filename, userId, userIdShare, password));
    }

    @Override
    public Result<byte[]> getFile(String filename, String userId, String accUserId, String password) {
        return super.reTry(() -> impl.getFile(filename, userId, accUserId, password));
    }

    @Override
    public Result<List<FileInfo>> lsFile(String userId, String password) {
        return super.reTry(() -> impl.lsFile(userId, password));
    }

    @Override
    public Result<Void> deleteUserFiles(String userId, String password, String token) {
        return super.reTry(() -> impl.deleteUserFiles(userId, password, token));
    }

    @Override
    public Result<Version> getVersion(String token) {
        return super.reTry(() -> impl.getVersion(token));
    }

    @Override
    public Result<Void> applyDelta(JavaDirectoryState.FileDelta delta, String token) {
        return super.reTry(() -> impl.applyDelta(delta, token));
    }

}
