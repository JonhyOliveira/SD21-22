package tp1.impl.servers.common.dropbox.commands;

import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.google.gson.Gson;
import tp1.api.service.java.Result;
import tp1.impl.servers.common.dropbox.util.DropboxContext;
import tp1.impl.servers.common.dropbox.util.Endpoints;

import java.util.HashMap;
import java.util.logging.Logger;

public class UpdateDropboxFile {

    private static final Logger Log = Logger.getLogger(ListDropboxDirectory.class.getName());
    private static final Gson json = new Gson();

    /**
     * Sets a Dropbox file contents to some data
     * @param context the Dropbox auth context
     * @param pathOrID the path or ID of the file
     * @param data the file data
     * @return A result containing the value returned by Dropbox. If successful value will be a file identifier.
     * @throws Exception if an error occurred while executing the request to the server
     */
    public static Result<String> execute(DropboxContext context, String pathOrID, byte[] data) throws Exception {
        var args = new UploadFileArgs(pathOrID);

        OAuthRequest oAuthRequest = Endpoints.UpdateFile.createUnsignedRequest(data);
        oAuthRequest.addHeader("Dropbox-API-Arg", json.toJson(args));
        context.signRequest(oAuthRequest);
        Response r = context.executeRequest(oAuthRequest);

        Log.warning("making request");

        if (r.getCode() != 200) {
            Log.warning(String.format("Failed to write file: %s, Status: %d, \nReason: %s\n", pathOrID, r.getCode(), r.getBody()));
            return Result.error(Result.ErrorCode.BAD_REQUEST,
                    new DropboxError(r.getCode(), json.fromJson(r.getBody(), JSONErrorFormat.class).toString()));
        }

        return Result.ok(json.fromJson(r.getBody(), ID.class).id());
    }


    private record UploadFileArgs(String path, String mode, boolean mute) {
        UploadFileArgs(String path) {
            this(path, "overwrite", true);
        }
    }

    private class ID extends HashMap<String, Object> {

        public static final String ID = "id";

        public String id() {
            return get(ID).toString();
        }

    }

}
