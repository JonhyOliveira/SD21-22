package tp1.impl.clients.rest;

import jakarta.ws.rs.core.UriBuilder;
import tp1.api.FileDelta;
import tp1.api.FileInfo;
import tp1.api.service.java.Directory;
import tp1.api.service.java.Result;
import tp1.api.service.rest.RestDirectory;
import tp1.impl.servers.common.JavaDirectoryState;

import java.net.URI;
import java.util.List;

import static tp1.api.service.rest.RestDirectory.SHARE;

public class RestDirectoryURIFactory {

    private URI serviceURI;

    public RestDirectoryURIFactory(URI serviceURI) {
        this.serviceURI = serviceURI;
    }

    public URI forWriteFile(String filename, byte[] data, String userId, String password) {
        return UriBuilder.fromUri(this.serviceURI)
                .path(userId)
                .path(filename)
                .queryParam(RestDirectory.PASSWORD, password)
                .build();
    }

    public URI forDeleteFile(String filename, String userId, String password) {
        return UriBuilder.fromUri(this.serviceURI)
                .path(userId)
                .path(filename)
                .queryParam(RestDirectory.PASSWORD, password)
                .build();
    }

    public URI forShareFile(String filename, String userId, String userIdShare, String password) {
        return UriBuilder.fromUri(this.serviceURI)
                .path(userId)
                .path(filename)
                .path(SHARE)
                .path(userIdShare)
                .queryParam(RestDirectory.PASSWORD, password)
                .build();
    }

    public URI forUnShareFile(String filename, String userId, String userIdShare, String password) {
        return UriBuilder.fromUri(this.serviceURI)
                .path(userId)
                .path(filename)
                .path(SHARE)
                .path(userIdShare)
                .queryParam(RestDirectory.PASSWORD, password)
                .build();
    }

    public URI forGetFile(String filename, String userId, String accUserId, String password) {
        return UriBuilder.fromUri(this.serviceURI)
                .path(userId)
                .path(filename)
                .queryParam(RestDirectory.ACC_USER_ID, accUserId)
                .queryParam(RestDirectory.PASSWORD, password)
                .build();
    }

    public URI forLsFile(String userId, String password) {
        return UriBuilder.fromUri(this.serviceURI)
                .path(userId)
                .queryParam(RestDirectory.PASSWORD, password)
                .build();
    }

    public URI forDeleteUserFiles(String userId, String password, String token) {
        return UriBuilder.fromUri(this.serviceURI)
                .path(userId)
                .queryParam(RestDirectory.PASSWORD, password)
                .queryParam(RestDirectory.TOKEN, token)
                .build();
    }

    public URI forGetVersion(String token) {
        return UriBuilder.fromUri(this.serviceURI)
                .queryParam(RestDirectory.TOKEN, token)
                .build();
    }

    public URI forSendDelta(FileDelta delta, String token) {
        return UriBuilder.fromUri(this.serviceURI)
                .queryParam(RestDirectory.TOKEN, token)
                .build();
    }
}
