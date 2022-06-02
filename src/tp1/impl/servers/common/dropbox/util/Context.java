package tp1.impl.servers.common.dropbox.util;

import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.oauth.OAuth20Service;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class Context {

    private final OAuth20Service service;
    private OAuth2AccessToken token;

    public Context(OAuth20Service service, OAuth2AccessToken accessToken) {
        this.service = service;
        this.token = accessToken;
    }

    public void signRequest(OAuthRequest request) {
        service.signRequest(this.token, request);
    }

    public Response executeRequest(OAuthRequest request) throws IOException, ExecutionException, InterruptedException {
        return service.execute(request);
    }

    public void setToken(OAuth2AccessToken token) {
        this.token = token;
    }
}
