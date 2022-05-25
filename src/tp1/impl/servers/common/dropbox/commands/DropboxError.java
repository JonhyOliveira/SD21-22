package tp1.impl.servers.common.dropbox.commands;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;

public record DropboxError(int statusCode, String descriptor) {
}

class JSONErrorFormat extends HashMap<String, Object> {

    private static final String SUMMARY = "error_summary";

    @Override
    public String toString() {
        return get(SUMMARY).toString();
    }
}
