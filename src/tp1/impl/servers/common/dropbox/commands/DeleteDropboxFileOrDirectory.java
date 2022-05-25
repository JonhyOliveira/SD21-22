package tp1.impl.servers.common.dropbox.commands;

import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.google.gson.Gson;
import tp1.api.service.java.Result;
import tp1.impl.servers.common.dropbox.util.DropboxContext;
import tp1.impl.servers.common.dropbox.util.Endpoints;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

public class DeleteDropboxFileOrDirectory {

    private static final Logger Log = Logger.getLogger(DeleteDropboxFileOrDirectory.class.getName());
    private static final Gson json = new Gson();

    public static Result<Void> execute(DropboxContext context, String path) throws IOException, ExecutionException, InterruptedException {
        var args = new DeleteArgs(path);

        OAuthRequest oAuthRequest = Endpoints.Delete.createUnsignedRequest(json.toJson(args));
        context.signRequest(oAuthRequest);
        Response r = context.executeRequest(oAuthRequest);

        if (r.getCode() != 200) {
            Log.warning(String.format("Failed to delete: %s, Status: %d, \nReason: %s\n", path, r.getCode(), r.getBody()));
            return Result.error(Result.ErrorCode.BAD_REQUEST, new DropboxError(r.getCode(), r.getBody()));
        }

        return Result.ok();
    }

    private record DeleteArgs(String path) {}

}
