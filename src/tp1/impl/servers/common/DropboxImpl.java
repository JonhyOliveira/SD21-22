package tp1.impl.servers.common;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.google.gson.Gson;
import org.pac4j.scribe.builder.api.DropboxApi20;
import tp1.api.service.java.Files;
import tp1.api.service.java.Result;
import tp1.impl.servers.common.dropbox.DropboxRestClient;
import tp1.impl.servers.common.dropbox.util.Context;
import tp1.impl.servers.common.dropbox.util.Preserve;
import util.Json;
import util.Token;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Logger;

/**
 * Implementation of the Files service using Dropbox as a storage device.
 * Files are saved as-is.
 */
public class DropboxImpl implements Files {

    private static final Logger Log = Logger.getLogger(DropboxImpl.class.getName());

    private static final String apiKey = "dlcfea3hujnjj3b";
    private static final String apiSecret = "6wifguey4f211u0";
    private static final String accessTokenStr = "sl.BI_RuxZOykErn5oY6-qg9G-AR_tlSuBD0RoLukYeDeShpTNe20B7XvzrEdviymle8hV_3f8jQNeMWH-0QGb121_9rJ5fn6SHhq7yMYIVjLW9S-ZSFfcrYTor-IC7VhjraInwz3Y";

    private static final String DELIMITER = JavaFiles.DELIMITER;
    private static final String BASE_DIR = "/tp1-21_22-SD";

    private final DropboxRestClient dropboxClient;

    // TODO isto devia de ser uma cache
    private final Set<String> files = new ConcurrentSkipListSet<>();
    private final Gson json = Json.getInstance();

    public DropboxImpl() {
        var service = new ServiceBuilder(apiKey).apiSecret(apiSecret).build(DropboxApi20.INSTANCE);
        dropboxClient = new DropboxRestClient(new Context(service, new OAuth2AccessToken(accessTokenStr)));
        Log.info("Initializing..");
        try {
            if (Preserve.get()) {
                Log.info("Preserving.");

                Log.info(String.valueOf(dropboxClient.createDirectory(BASE_DIR, true).isOK())); // create if does not exist

                var res = dropboxClient.listDirectory(BASE_DIR);

                Log.info(String.valueOf(res.isOK()));
                Collection<String> fetchedFileNames;

                if (res.isOK())
                    fetchedFileNames = res.value();
                else if (res.errorValue() instanceof Collection) {
                    fetchedFileNames = res.errorValue();
                } else {
                    fetchedFileNames = Collections.emptyList();
                }

                files.addAll(fetchedFileNames);

            } else {
                Log.info("Not preserving.");
                Log.info(String.valueOf(dropboxClient.delete(BASE_DIR).isOK()));
                Log.info(String.valueOf(dropboxClient.createDirectory(BASE_DIR, false).isOK()));
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.severe("Error initializing service. Can't proceed.");
            System.exit(-1);
        }

        Log.info("\rDone.........");
    }

    @Override
    public Result<byte[]> getFile(String fileId, String token) {

        verifyToken(userId(fileId), token);

        var res = dropboxClient.getFile("%s/%s".formatted(BASE_DIR, fileId));

        if (res.isOK())
            this.files.add(fileId);

        return res;
    }

    @Override
    public Result<Void> deleteFile(String fileId, String token) {

        verifyToken(userId(fileId), token);

        var res = dropboxClient.delete("%s/%s".formatted(BASE_DIR, fileId));

        if (res.isOK())
            this.files.remove(fileId);

        return res;
    }

    @Override
    public Result<Void> writeFile(String fileId, byte[] data, String token) {

        verifyToken(userId(fileId), token);

        var res = dropboxClient.uploadFile("%s/%s".formatted(BASE_DIR, fileId), data);

        if (res.isOK()) {
            return Result.ok();
        } else
            return Result.error(res.error(), res.errorValue());

    }

    @Override
    public Result<Void> deleteUserFiles(String userId, String token) {

        verifyToken(userId, token);

        if (files.stream()
                .filter(fileID -> fileID.startsWith(userId + DELIMITER))
                .allMatch(fileID -> deleteFile(fileID, token).isOK()))
            return Result.ok();
        else
            return Result.error(Result.ErrorCode.FORBIDDEN);
    }

    private String userId(String fileId) {
        return fileId.split(DELIMITER, 2)[0];
    }

    /**
     * Checks if the token was created with the common secret
     *
     * @param token the token with unknown validity
     * @return true if the token was created with the common secret and is valid, false otherwise
     */
    private boolean verifyToken(String token) {
        return Token.get().equals(token);
    }

    /**
     * Checks the validity of a token and it's owner
     *
     * @param userID the owner of the token
     * @param token  the token
     * @return true if the token is valid, false otherwise
     */
    private boolean verifyToken(String userID, String token) {
        return verifyToken(token);
    }


}
