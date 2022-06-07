package tp1.api.service.rest;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import tp1.api.FileDelta;
import tp1.api.FileInfo;

import java.util.List;

@Path(RestDirectory.PATH)
public interface RestDirectory {

    String PATH = "/dir";
    String TOKEN = "token";
    String USER_ID = "userId";
    String FILENAME = "filename";
    String PASSWORD = "password";
    String ACC_USER_ID = "accUserId";
    String USER_ID_SHARE = "userIdShare";
    String VERSION_HEADER = "X-DFS-Versao";
    String SHARE = "share";

    /**
     * Write a new version of a file. If the file exists, its contents are
     * overwritten. Only the owner (userId) can write the file.
     * <p>
     * A file resource will has the full path "userId/filename".
     *
     * @param version
     * @param filename - name of the file.
     * @param data     - contents of the file.
     * @param userId   - id of the user.
     * @param password - the password of the user.
     * @return 200 if success + FileInfo representing the file. 404 if the userId
     * does not exist. 403 if the password is incorrect. 400 otherwise.
     */
    @POST
    @Path("/{" + USER_ID + "}/{" + FILENAME + "}")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    FileInfo writeFile(@HeaderParam(VERSION_HEADER) String version, @PathParam(FILENAME) String filename, byte[] data, @PathParam(USER_ID) String userId,
                       @QueryParam(PASSWORD) String password);

    /**
     * Delete an existing file ("userId/filename"). Only the owner (userId) can
     * delete the file.
     *
     * @param version
     * @param filename - name of the file.
     * @param userId   - id of the user.
     * @param password - the password of the user.
     * @return 204 if success; 404 if the userId or filename does not exist. 403 if
     * the password is incorrect. 400 otherwise.
     */
    @DELETE
    @Path("/{" + USER_ID + "}/{" + FILENAME + "}")
    void deleteFile(@HeaderParam(VERSION_HEADER) String version, @PathParam(FILENAME) String filename, @PathParam(USER_ID) String userId,
                    @QueryParam(PASSWORD) String password);

    /**
     * Share the file "userId/filename" with another user. Only the owner (userId)
     * can share the file.
     * <p>
     * The operation succeeds if and only if the userId, userIdShare and
     * userId/filename exist and password is correct (note: if userIdShare already
     * has access to the file, the operation still succeeds).
     *
     * @param version
     * @param filename    - name of the file.
     * @param userId      - id of the user.
     * @param userIdShare - id of the user to share the file with.
     * @param password    - the password of the user.
     * @return 204 if success; 404 if the userId or userIdShare or filename does not
     * exist. 403 if the password is incorrect. 400 otherwise.
     */
    @POST
    @Path("/{" + USER_ID + "}/{" + FILENAME + "}/"+ SHARE + "/{" + USER_ID_SHARE + "}")
    void shareFile(@HeaderParam(VERSION_HEADER) String version, @PathParam(FILENAME) String filename, @PathParam(USER_ID) String userId,
                   @PathParam(USER_ID_SHARE) String userIdShare, @QueryParam(PASSWORD) String password);

    /**
     * Unshare the file "userId/filename" with another user. Only the owner (userId)
     * can unshare the file.
     * <p>
     * The operation succeeds if and only if the userId, userIdShare and
     * userId/filename exist and password is correct (note: if userIdShare does not
     * have access to the file, the operation still succeeds).
     *
     * @param version
     * @param filename    - name of the file.
     * @param userId      - id of the user.
     * @param userIdShare - id of the user to unshare the file with.
     * @param password    - the password of the user.
     * @return 204 if success; 404 if the userId or userIdShare or filename does not
     * exist. 403 if the password is incorrect. 400 otherwise.
     */
    @DELETE
    @Path("/{" + USER_ID + "}/{" + FILENAME + "}/"+ SHARE + "/{" + USER_ID_SHARE + "}")
    void unshareFile(@HeaderParam(VERSION_HEADER) String version, @PathParam(FILENAME) String filename, @PathParam(USER_ID) String userId,
                     @PathParam(USER_ID_SHARE) String userIdShare, @QueryParam(PASSWORD) String password);

    /**
     * Get the contents of the file "userId/filename". Who can read a file: the
     * owner and the users with whom the file has been shared.
     * <p>
     * This operation should be implemented using HTTP redirect on success.
     *
     * @param version
     * @param filename  - name of the file.
     * @param userId    - id of the user.
     * @param accUserId - id of the user executing the operation.
     * @param password  - the password of accUserId.
     * @return 200 if success + contents (through redirect to the File server); 404
     * if the userId or filename or accUserId does not exist. 403 if the
     * password is incorrect or the user cannot access the file. 400
     * otherwise.
     */
    @GET
    @Path("/{" + USER_ID + "}/{" + FILENAME + "}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    byte[] getFile(@HeaderParam(VERSION_HEADER) String version, @PathParam(FILENAME) String filename, @PathParam(USER_ID) String userId,
                   @QueryParam(ACC_USER_ID) String accUserId, @QueryParam(PASSWORD) String password);

    /**
     * List the files a given user ("userId") has access to - this includes both its
     * own files and the files shared with her.
     *
     * @param version
     * @param userId   - id of the user.
     * @param password - the password of the user.
     * @return 200 if success + list of FileInfo; 404 if the userId does not exist.
     * 403 if the password is incorrect. 400 otherwise.
     */
    @GET
    @Path("/{" + USER_ID + "}")
    @Produces(MediaType.APPLICATION_JSON)
    List<FileInfo> lsFile(@HeaderParam(VERSION_HEADER) String version, @PathParam(USER_ID) String userId, @QueryParam(PASSWORD) String password);

    @DELETE
    @Path("/{" + USER_ID + "}")
    void deleteUserFiles(@HeaderParam(VERSION_HEADER) String version, @PathParam(USER_ID) String userId, @QueryParam(PASSWORD) @DefaultValue("") String password, @QueryParam(TOKEN) String token);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    String getVersion(@QueryParam(TOKEN) String token);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    void applyDelta(@HeaderParam(VERSION_HEADER) String version, @QueryParam(TOKEN) String token, FileDelta delta);

}
