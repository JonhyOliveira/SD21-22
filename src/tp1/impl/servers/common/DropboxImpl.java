package tp1.impl.servers.common;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import org.pac4j.scribe.builder.api.DropboxApi20;
import tp1.api.service.java.Files;
import tp1.api.service.java.Result;
import tp1.impl.servers.common.dropbox.commands.*;
import tp1.impl.servers.common.dropbox.util.DropboxContext;
import tp1.impl.service.rest.DropboxServer;

import java.net.URL;
import java.util.*;
import java.util.logging.Logger;

/**
 * Implementation of the Files service using Dropbox as a storage device.
 * Files are saved as-is.
 */
public class DropboxImpl implements Files {

    private static final Logger Log = Logger.getLogger(DropboxImpl.class.getName());

    private static final String apiKey = "dlcfea3hujnjj3b";
    private static final String apiSecret = "6wifguey4f211u0";
    private static final String accessTokenStr = "sl.BIDt4srARJjJ3TGsmt2VP9hPPJz74eIlUrYq44HDkCi8MqxPYL-b4bHyMbHu21xrYZfxPoGyMChja8hGiaEcWfBtvY3ma5RNlBCdAAZfWL9Dg05md-oqbdujNCEBwtbNdCnBNlU";

    private static final String DELIMITER = JavaFiles.DELIMITER;
    private static final String BASE_DIR = "/tp1-21_22-SD";
    
    private final DropboxContext context;

    private Map<String, String> files;

    public DropboxImpl() {
        var service = new ServiceBuilder(apiKey).apiSecret(apiSecret).build(DropboxApi20.INSTANCE);
        context = new DropboxContext(service, new OAuth2AccessToken(accessTokenStr));

        try {
            if (DropboxServer.PRESERVE) {
                assert CreateDropboxDirectory.execute(context, BASE_DIR, true).isOK(); // try to create

                var res = ListDropboxDirectory.execute(context, BASE_DIR);

                Collection<String> f;

                if (res.isOK())
                    f = res.value();
                else if (res.errorValue() instanceof ListDropboxDirectory.Error) {
                        f = ((ListDropboxDirectory.Error) res.errorValue()).partial();
                }
                else {
                    f = Collections.emptyList();
                }


                f.forEach(s -> files.put(s, s));


            } else {
                DeleteDropboxFileOrDirectory.execute(context, BASE_DIR).isOK();
                CreateDropboxDirectory.execute(context, BASE_DIR, false).isOK();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.severe("Error initializing service. Can't proceed.");
            System.exit(-1);
        }


    }

    @Override
    public Result<byte[]> getFile(String fileId, String token) {

        if (!files.containsKey(fileId)) // not in cache. cant retrieve
            return Result.error(Result.ErrorCode.NOT_FOUND, "not in cache");

        try {
            return DownloadDropboxFile.execute(context, BASE_DIR + "/" + files.get(fileId));

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(Result.ErrorCode.INTERNAL_ERROR, e);
        }
    }

    @Override
    public Result<Void> deleteFile(String fileId, String token) {

        try {
            var res = DeleteDropboxFileOrDirectory.execute(context, BASE_DIR + "/" + files.get(fileId));

            if (res.isOK())
                files.remove(fileId);

            return res;

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(Result.ErrorCode.INTERNAL_ERROR, e);
        }

    }

    @Override
    public Result<Void> writeFile(String fileId, byte[] data, String token) {

        try {
            var res = UpdateDropboxFile.execute(context, BASE_DIR + "/" + fileId, data);

            Result<Void> ret;

            if (res.isOK()) {
                files.put(fileId, res.value());
                ret = Result.ok();
            }
            else
                ret = Result.error(Result.ErrorCode.BAD_REQUEST);

            return ret;

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(Result.ErrorCode.INTERNAL_ERROR, e);
        }


    }

    @Override
    public Result<Void> deleteUserFiles(String userId, String token) {

        return null;
    }

}
