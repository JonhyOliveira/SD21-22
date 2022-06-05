package tp1.impl.servers.common.dropbox;

import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.google.gson.Gson;
import com.google.gson.JsonParser;
import tp1.api.service.java.Result;
import tp1.impl.servers.common.dropbox.util.Context;
import tp1.impl.servers.common.dropbox.util.Endpoints;
import tp1.impl.servers.common.dropbox.util.Header;
import util.Json;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DropboxRestClient {

    private static final Gson json = Json.getInstance();
    private static final Logger Log = Logger.getLogger(DropboxRestClient.class.getName());

    private static final String DROPBOX_ARG_HEADER = "Dropbox-API-Arg";

    private final Context context;

    public DropboxRestClient(Context context) {
        this.context = context;
    }

    public Result<Void> createDirectory(String directoryPath, boolean expectConflict) {
        var args = json.toJson(Map.of("path", directoryPath, "autorename", false));

        var request = handleRequest(Endpoints.CreateDirectory.createUnsignedRequest(args),
                args, "creating directory");

        if (request.isOK())
            return Result.ok();
        else {
            if (request.errorValue() instanceof String && expectConflict) {
                boolean isConflict = !((String) request.errorValue()).contains("path/conflict");
                if (isConflict)
                    return Result.ok();
            }

            return Result.error(request.error(), request.errorValue());
        }

    }

    public Result<Collection<String>> listDirectory(String basePath) {
        Set<String> files = new HashSet<>();
        var args = json.toJson(new ListFolderArgs(basePath));

        var result = handleRequest(Endpoints.ListDirectory.createUnsignedRequest(args), args, "listing directory");

        if (!result.isOK())
            return Result.error(result.error(), result.errorValue());

        ListFolderReturn reply;
        do {
            reply = json.fromJson(result.value(), ListFolderReturn.class);
            reply.getEntries().stream().filter(ListFolderReturn.FolderEntry::isFile)
                    .map(ListFolderReturn.FolderEntry::getID).forEach(files::add);

            result = handleRequest(Endpoints.ListDirectory.createUnsignedRequest(args), args, "listing directory");

            if (!result.isOK())
                return Result.error(result.error(), files);

        } while (reply.has_more());

        return Result.ok(files);

    }

    private record ListFolderArgs(String path, boolean recursive, boolean include_media_info, boolean include_deleted,
                                  boolean include_has_explicit_shared_members, boolean include_mounted_folders) {
        public ListFolderArgs(String path) {
            this(path, false, false, false, false, false);
        }
    }

    private record ListFolderContinueArgs(String cursor) {
    }

    private static class ListFolderReturn {

        private String cursor;
        private boolean has_more;
        private List<ListFolderReturn.FolderEntry> entries;

        public ListFolderReturn() {
        }

        public String getCursor() {
            return cursor;
        }

        public void setCursor(String cursor) {
            this.cursor = cursor;
        }

        public boolean has_more() {
            return has_more;
        }

        public void setHas_more(boolean has_more) {
            this.has_more = has_more;
        }

        public List<ListFolderReturn.FolderEntry> getEntries() {
            return entries;
        }

        public void setEntries(List<ListFolderReturn.FolderEntry> entries) {
            this.entries = entries;
        }

        public Map<String, String> getFiles() {
            return getEntries().stream().filter(ListFolderReturn.FolderEntry::isFile)
                    .collect(Collectors.toMap(ListFolderReturn.FolderEntry::getName, ListFolderReturn.FolderEntry::getID));
        }

        public static class FolderEntry extends HashMap<String, Object> {
            private static final long serialVersionUID = 1L;
            private static final String NAME = "name", TYPE = ".tag", ID = "id";

            public boolean isFile() {
                return get(TYPE).toString().equals("file");
            }

            public String getName() {
                return get(NAME).toString();
            }

            public String getID() {
                return get(ID).toString();
            }
        }
    }

    /**
     * Sets a Dropbox file contents to some data
     *
     * @param filePathOrID the path or ID of the file
     * @param data         the file data
     * @return The file identifier or error information
     */
    public Result<String> uploadFile(String filePathOrID, byte[] data) {
        var args = json.toJson(Map.of("path", filePathOrID, "mode", "overwrite", "mute", true));

        Log.fine("DROPBOX: Writing file. Args: %s".formatted(args));

        var request = executeDropboxRequest(Endpoints.UpdateFile
                .createUnsignedRequest(data, List.of(new Header(DROPBOX_ARG_HEADER, args))));

        if (request.isOK())
            return Result.ok(JsonParser.parseString(request.value()).getAsJsonObject().get("id").getAsString());
        else
            return Result.error(request.error(), request.errorValue());
    }

    /**
     * Deletes a Dropbox file or folder
     *
     * @param pathOrID the path or ID of the thing to delete
     * @return null or error information
     */
    public Result<Void> delete(String pathOrID) {
        var args = json.toJson(Map.of("path", pathOrID));


        var request = handleRequest(Endpoints.Delete
                .createUnsignedRequest(args), args, "deleting");

        if (request.isOK())
            return Result.ok();
        else {
            return Result.error(request.error(), request.errorValue());
        }
    }

    /**
     * @param filePathOrID the path or id of the file
     * @return the contents of the file or error information
     */
    public Result<byte[]> getFile(String filePathOrID) {
        var args = json.toJson(Map.of("path", filePathOrID));

        var request = handleRequestRaw(Endpoints.DownloadFile
                        .createUnsignedRequest(List.of(new Header(DROPBOX_ARG_HEADER, args),
                                new Header(Header.CONTENT_TYPE, "application/octet-stream; charset=utf-8"))),
                args, "getting file contents");

        if (request.isOK()) {
            try {
                return Result.ok(request.value().getStream().readAllBytes());
            } catch (IOException e) {
                return Result.error(Result.ErrorCode.INTERNAL_ERROR, "error reading byte stream");
            }
        } else {
            return Result.error(request.error(), request.errorValue());
        }
    }

    private Result<Response> handleRequestRaw(OAuthRequest request, String args, String action) {

        Log.fine("DROPBOX: %s. Args: %s".formatted(action.toUpperCase(), args));
        var r = executeDropboxRequestRaw(request);

        if (r.isOK())
            return r;
        else
            return (Result<Response>) handleError(r, args, action);
    }

    private Result<String> handleRequest(OAuthRequest request, String args, String action) {

        Log.fine("DROPBOX: %s. Args: %s".formatted(action.toUpperCase(), args));
        var r = executeDropboxRequest(request);

        if (r.isOK())
            return r;
        else
            return (Result<String>) handleError(r, args, action);
    }

    private Result<?> handleError(Result<?> errorResult, String args, String action) throws IllegalArgumentException {
        if (errorResult.isOK())
            throw new IllegalArgumentException("Result can not be OK");

        var error = errorResult.errorValue();
        if (error instanceof Throwable) {
            Log.warning("DROPBOX: Failed at %s: %s, Status: %s,\nReason: %s\n"
                    .formatted(action, args, errorResult.errorValue(), ((Throwable) error).getMessage()));
        } else {
            Log.warning("DROPBOX: Failed at %s: %s, Status: %s,\nReason: %s\n"
                    .formatted(action, args, errorResult.errorValue(), error));
            error = new Exception(error.toString());
        }
        return Result.error(errorResult.error(), error);
    }

    private Result<Response> executeDropboxRequestRaw(OAuthRequest request) {
        context.signRequest(request);

        try {
            Response r;
            r = context.executeRequest(request);

            if (r.getCode() != 200) {
                return Result.error(Result.ErrorCode.INTERNAL_ERROR, r);
            }

            return Result.ok(r);

        } catch (IOException | ExecutionException | InterruptedException e) {
            return Result.error(Result.ErrorCode.TIMEOUT, e);
        } catch (Exception e) {
            return Result.error(Result.ErrorCode.INTERNAL_ERROR, e);
        }
    }

    private Result<String> executeDropboxRequest(OAuthRequest request) {
        context.signRequest(request);

        try {
            Response r;
            r = context.executeRequest(request);

            try {
                if (r.getCode() != 200) {
                    return Result.error(Result.ErrorCode.INTERNAL_ERROR, r.getBody());
                }

                return Result.ok(r.getBody());
            } catch (IOException e) { // when reading body
                return Result.error(Result.ErrorCode.INTERNAL_ERROR, e);
            }

        } catch (IOException | ExecutionException | InterruptedException e) {
            return Result.error(Result.ErrorCode.TIMEOUT, e);
        } catch (Exception e) {
            return Result.error(Result.ErrorCode.INTERNAL_ERROR, e);
        }

    }

}
