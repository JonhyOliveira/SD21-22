package tp1.impl.servers.common;

import tp1.api.FileInfo;
import tp1.api.service.java.Result;
import util.kafka.SyncPoint;

import java.net.URI;
import java.util.Map;

import static tp1.api.service.java.Result.ErrorCode.*;
import static tp1.api.service.java.Result.error;
import static tp1.api.service.java.Result.ok;

public class JavaDirectoryReplicated extends JavaDirectory {

    public JavaDirectoryReplicated() {
        super();
    }

    @Override
    public Result<FileInfo> writeFile(String filename, byte[] data, String userId, String password) {

        if (badParam(filename) || badParam(userId))
            return error(BAD_REQUEST);

        var user = getUser(userId, password);
        if (!user.isOK())
            return error(user.error());

        return ok();

    }

    @Override
    public Result<Void> deleteFile(String filename, String userId, String password) {

        if (badParam(filename) || badParam(userId))
            return error(BAD_REQUEST);

        var fileId = fileId(filename, userId);

        var file = files.get(fileId);
        if (file == null)
            return error(NOT_FOUND);

        var user = getUser(userId, password);
        if (!user.isOK())
            return error(user.error());

        return ok();
    }

    @Override
    public Result<Void> shareFile(String filename, String userId, String userIdShare, String password) {
        if (badParam(filename) || badParam(userId) || badParam(userIdShare))
            return error(BAD_REQUEST);

        var fileId = fileId(filename, userId);

        var file = files.get(fileId);
        if (file == null || getUser(userIdShare, "").error() == NOT_FOUND)
            return error(NOT_FOUND);

        var user = getUser(userId, password);
        if (!user.isOK())
            return error(user.error());

        return ok();
    }

    @Override
    public Result<Void> unshareFile(String filename, String userId, String userIdShare, String password) {
        return this.shareFile(filename, userId, userIdShare, password);
    }

    @Override
    public Result<byte[]> getFile(String filename, String userId, String accUserId, String password) {
        if (badParam(filename))
            return error(BAD_REQUEST);

        var fileId = fileId(filename, userId);
        var file = files.get(fileId);
        if (file == null)
            return error(NOT_FOUND);

        var user = getUser(accUserId, password);
        if (!user.isOK())
            return error(user.error());

        if (!file.info().hasAccess(accUserId))
            return error(FORBIDDEN);

        return ok();
    }

    public Map<String, ExtendedFileInfo> files() {
        return this.files;
    }

    public Map<String, UserFiles> userFiles() {
        return this.userFiles;
    }

    public Map<URI, FileCounts> fileCounts() {
        return this.fileCounts;
    }

}
