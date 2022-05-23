package tp1.impl.service.common.dropbox.commands;

import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.google.gson.Gson;
import tp1.api.service.java.Result;
import tp1.impl.service.common.dropbox.util.DropboxContext;
import tp1.impl.service.common.dropbox.util.Endpoints;
import tp1.impl.service.common.dropbox.util.Header;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

public class DownloadDropboxFile {

    private static final Logger Log = Logger.getLogger(CreateDropboxDirectory.class.getName());
    private static final Gson json = new Gson();

    public static Result<byte[]> execute(DropboxContext context, String path) throws IOException, ExecutionException, InterruptedException {
        var args = new DownloadFileArgs(path);

        OAuthRequest oAuthRequest = Endpoints.DownloadFile
                .createUnsignedRequest(List.of(new Header(Header.DROPBOX_ARG_HEADER, json.toJson(args))));
        context.signRequest(oAuthRequest);
        Response r = context.executeRequest(oAuthRequest);

        if (r.getCode() != 200) {
            Log.warning(String.format("Failed to download file: %s, Status: %d, \nReason: %s\n", path, r.getCode(), r.getBody()));
            return Result.error(Result.ErrorCode.BAD_REQUEST,
                    new DropboxError(r.getCode(), json.fromJson(r.getBody(), JSONErrorFormat.class).toString()));
        }

        return Result.ok(r.getStream().readAllBytes());
    }

    private record DownloadFileArgs(String path) {}

}
