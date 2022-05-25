package tp1.impl.servers.common.dropbox.commands;

import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.google.gson.Gson;
import tp1.api.service.java.Result;
import tp1.impl.servers.common.dropbox.util.Endpoints;
import tp1.impl.servers.common.dropbox.util.DropboxContext;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

public class CreateDropboxDirectory {

    private static final Logger Log = Logger.getLogger(CreateDropboxDirectory.class.getName());
    private static final Gson json = new Gson();

    /**
     * Creates a Dropbox folder
     * @param context the dropbox auth context
     * @param directoryPath the path to the directory to create
     * @param expectConflict if conflicts should be expected or not (if true, conflicts will not result in error)
     * @return the result of this request
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public static Result<Void> execute(DropboxContext context, String directoryPath, boolean expectConflict) throws IOException, ExecutionException, InterruptedException {
        var args = new CreateFolderV2Args(directoryPath);

        OAuthRequest oAuthRequest = Endpoints.CreateDirectory.createUnsignedRequest(json.toJson(args));
        context.signRequest(oAuthRequest);
        Response r = context.executeRequest(oAuthRequest);

        if (r.getCode() != 200) {
            Log.warning(String.format("Failed to create directory: %s, Status: %d, \nReason: %s\n", directoryPath, r.getCode(), r.getBody()));
            var error = new DropboxError(r.getCode(), json.fromJson(r.getBody(), JSONErrorFormat.class).toString());

            boolean isConflict = !error.descriptor().startsWith("path/conflict");

            if (!expectConflict || !isConflict)
                return Result.error(Result.ErrorCode.BAD_REQUEST, error);
        }

        return Result.ok();
    }



    private record CreateFolderV2Args(String path, boolean autorename) {
        public CreateFolderV2Args(String path) {
            this(path, false);
        }
    }
}
