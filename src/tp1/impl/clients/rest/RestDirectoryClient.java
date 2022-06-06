package tp1.impl.clients.rest;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tp1.api.FileInfo;
import tp1.api.service.java.Directory;
import tp1.api.service.java.Result;
import tp1.api.service.rest.RestDirectory;
import tp1.impl.servers.common.JavaDirectoryState;
import tp1.impl.servers.common.replication.Version;

import java.net.URI;
import java.util.List;

import static tp1.api.service.rest.RestDirectory.SHARE;

public class RestDirectoryClient extends RestClient implements Directory {

    public RestDirectoryClient(URI serverUri) {
        super(serverUri, RestDirectory.PATH);
    }

    @Override
    public Result<FileInfo> writeFile(String filename, byte[] data, String userId, String password) {
        Response r = target.path(userId)
                .path(filename)
                .queryParam(RestDirectory.PASSWORD, password)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .post(Entity.entity(data, MediaType.APPLICATION_OCTET_STREAM));
        return super.toJavaResult(r, new GenericType<FileInfo>() {
        });
    }

    @Override
    public Result<Void> deleteFile(String filename, String userId, String password) {
        Response r = target.path(userId)
                .path(filename)
                .queryParam(RestDirectory.PASSWORD, password)
                .request()
                .delete();
        return super.toJavaResult(r);
    }

    @Override
    public Result<Void> shareFile(String filename, String userId, String userIdShare, String password) {
        Response r = target.path(userId)
                .path(filename)
                .path(SHARE)
                .path(userIdShare)
                .queryParam(RestDirectory.PASSWORD, password)
                .request()
                .post(Entity.json(null));
        return super.toJavaResult(r);
    }

    @Override
    public Result<Void> unshareFile(String filename, String userId, String userIdShare, String password) {
        Response r = target.path(userId)
                .path(filename)
                .path(SHARE)
                .path(userIdShare)
                .queryParam(RestDirectory.PASSWORD, password)
                .request()
                .delete();
        return super.toJavaResult(r);
    }

    @Override
    public Result<byte[]> getFile(String filename, String userId, String accUserId, String password) {
        Response r = target.path(userId)
                .path(filename)
                .queryParam(RestDirectory.ACC_USER_ID, accUserId)
                .queryParam(RestDirectory.PASSWORD, password)
                .request()
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .get();
        return super.toJavaResult(r, new GenericType<byte[]>() {
        });
    }

    @Override
    public Result<List<FileInfo>> lsFile(String userId, String password) {
        Response r = target.path(userId)
                .queryParam(RestDirectory.PASSWORD, password)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get();
        return super.toJavaResult(r, new GenericType<List<FileInfo>>() {
        });
    }

    @Override
    public Result<Void> deleteUserFiles(String userId, String password, String token) {
        Response r = target.path(userId)
                .queryParam(RestDirectory.PASSWORD, password)
                .queryParam(RestDirectory.TOKEN, token)
                .request()
                .delete();
        return super.toJavaResult(r);
    }

    @Override
    public Result<Version> getVersion(String token) {
        Response r = target
                .queryParam(RestDirectory.TOKEN, token)
                .request()
                .get();

        return super.toJavaResult(r, new GenericType<Version>() {});
    }

    @Override
    public Result<Void> applyDelta(JavaDirectoryState.FileDelta delta, String token) {
        Response r = target
                .queryParam(RestDirectory.TOKEN, token)
                .request()
                .post(Entity.entity(delta, MediaType.APPLICATION_JSON));

        return super.toJavaResult(r);
    }
}
