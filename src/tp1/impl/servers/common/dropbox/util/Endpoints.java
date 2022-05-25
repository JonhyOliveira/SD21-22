package tp1.impl.service.common.dropbox.util;

import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Verb;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public enum Endpoints {

    CreateDirectory("https://api.dropboxapi.com/2/files/create_folder_v2"),
    ListDirectory("https://api.dropboxapi.com/2/files/list_folder"),
    ListDirectoryContinue("https://api.dropboxapi.com/2/files/list_folder/continue"),
    UpdateFile("https://content.dropboxapi.com/2/files/upload"),
    Delete("https://api.dropboxapi.com/2/files/delete_v2"),
    DownloadFile("https://content.dropboxapi.com/2/files/download");

    public final String endpoint;
    private final List<Header> commandPredefinedHeaders;

    Endpoints(String endpoint) {
        this(endpoint, Collections.emptyList());
    }

    Endpoints(String endpoint, List<Header> predefinedHeaders) {
        this.endpoint = endpoint;
        this.commandPredefinedHeaders = List.copyOf(predefinedHeaders);
    }

    public OAuthRequest createUnsignedRequest() {
        return this.createUnsignedRequest(Collections.emptyList());
    }

    public OAuthRequest createUnsignedRequest(List<Header> otherHeaders) {
        var request = new OAuthRequest(Verb.POST, this.endpoint);

        var headers = new HashSet<Header>(); // no duplicates :)
        headers.addAll(otherHeaders);
        headers.addAll(commandPredefinedHeaders);
        headers.forEach(header -> request.addHeader(header.property(), header.value()));

        return request;
    }

    public OAuthRequest createUnsignedRequest(String payload) {
        return this.createUnsignedRequest(payload, Collections.emptyList());
    }

    public OAuthRequest createUnsignedRequest(String payload, List<Header> otherHeaders) {
        var request = new OAuthRequest(Verb.POST, this.endpoint);

        var headers = new HashSet<Header>(); // no duplicates :)
        headers.add(new Header("Content-Type", "application/json; charset=utf-8"));
        headers.addAll(otherHeaders);
        headers.addAll(commandPredefinedHeaders);
        headers.forEach(header -> request.addHeader(header.property(), header.value()));
        request.setPayload(payload);

        return request;
    }

    public OAuthRequest createUnsignedRequest(byte[] payload) {
        return this.createUnsignedRequest(payload, Collections.emptyList());
    }

    public OAuthRequest createUnsignedRequest(byte[] payload, List<Header> otherHeaders) {
        var request = new OAuthRequest(Verb.POST, this.endpoint);

        var headers = new HashSet<Header>(); // no duplicates :)
        headers.add(new Header("Content-Type", "application/octet-stream"));
        headers.addAll(otherHeaders);
        headers.addAll(commandPredefinedHeaders);
        headers.forEach(header -> request.addHeader(header.property(), header.value()));

        request.setPayload(payload);

        return request;
    }


}

