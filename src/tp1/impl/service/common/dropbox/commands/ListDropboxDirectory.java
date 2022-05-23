package tp1.impl.service.common.dropbox.commands;

import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.google.gson.Gson;
import tp1.api.service.java.Result;
import tp1.impl.service.common.dropbox.util.Endpoints;
import tp1.impl.service.common.dropbox.util.DropboxContext;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ListDropboxDirectory {

    private static final Logger Log = Logger.getLogger(ListDropboxDirectory.class.getName());
    private static final Gson json = new Gson();

    /**
     * Lists all files inside a dropbox folder
     * @param directoryPath the directory to list
     * @return the result of the remote operation
     * @throws Exception when executing the request
     */
    public static Result<Set<String>> execute(DropboxContext context, String directoryPath) throws IOException, ExecutionException, InterruptedException {
        Set<String> files = new HashSet<>();

        var args = new ListFolderArgs(directoryPath);

        OAuthRequest oAuthRequest = Endpoints.ListDirectory.createUnsignedRequest(json.toJson(args));
        context.signRequest(oAuthRequest);
        Response r = context.executeRequest(oAuthRequest);

        if (r.getCode() != 200) {
            Log.warning(String.format("Failed to list directory: %s, Status: %d, \nReason: %s\n", directoryPath, r.getCode(), r.getBody()));
            return Result.error(Result.ErrorCode.BAD_REQUEST,
                    new Error(new DropboxError(r.getCode(), json.fromJson(r.getBody(), JSONErrorFormat.class).toString()), files)
            );
        }

        var reply = json.fromJson(r.getBody(), ListFolderReturn.class);
        reply.getEntries().stream().map(ListFolderReturn.FolderEntry::toString).forEach(files::add);

        while (reply.has_more()) {

            oAuthRequest = Endpoints.ListDirectoryContinue.createUnsignedRequest(json.toJson(new ListFolderContinueArgs(reply.getCursor())));
            context.signRequest(oAuthRequest);

            r = context.executeRequest(oAuthRequest);

            if (r.getCode() != 200) {
                Log.warning(String.format("Failed to continue to list directory: %s, Status: %d, \nReason: %s\n", directoryPath, r.getCode(), r.getBody()));
                return Result.error(Result.ErrorCode.BAD_REQUEST,
                        new Error(new DropboxError(r.getCode(), json.fromJson(r.getBody(), JSONErrorFormat.class).toString()), files)
                );
            }

            reply = json.fromJson(r.getBody(), ListFolderReturn.class);
            reply.getEntries().stream().map(ListFolderReturn.FolderEntry::toString).forEach(files::add);
        }

        return Result.ok(files);
    }

    /**
     * @param descriptor description of the error
     * @param partial the partial contents of the directory that could be retrieved
     */
    public record Error(DropboxError descriptor, Collection<String> partial) {}

    private record ListFolderArgs( String path, boolean recursive, boolean include_media_info, boolean include_deleted, boolean include_has_explicit_shared_members, boolean include_mounted_folders) {
        public ListFolderArgs( String path) {
            this( path, false, false, false, false, false);
        }
    }

    private record ListFolderContinueArgs(String cursor) { }

    private static class ListFolderReturn {

        private String cursor;
        private boolean has_more;
        private List<FolderEntry> entries;

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

        public List<FolderEntry> getEntries() {
            return entries;
        }

        public void setEntries(List<FolderEntry> entries) {
            this.entries = entries;
        }

        public Map<String, String> getFiles() {
            return getEntries().stream().filter(FolderEntry::isFile)
                    .collect(Collectors.toMap(FolderEntry::getName, FolderEntry::getID));
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

}


