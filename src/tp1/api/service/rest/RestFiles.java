package tp1.api.service.rest;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path(RestFiles.PATH)
public interface RestFiles {

    String PATH = "/files";
    String TOKEN = "token";
    String FILE_ID = "fileId";
    String USER_ID = "userId";

    /**
     * Write a file. If the file exists, overwrites the contents.
     *
     * @param fileId - unique id of the file.
     * @param token  - token for accessing the file server (in the first project
     *               this will not be used).
     * @return 204 if success. 403 if the password is incorrect. 400 otherwise.
     */
    @POST
    @Path("/{" + FILE_ID + "}")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    void writeFile(@PathParam(FILE_ID) String fileId, byte[] data, @QueryParam(TOKEN) @DefaultValue("") String token);

    /**
     * Delete an existing file.
     *
     * @param fileId - unique id of the file.
     * @param token  - token for accessing the file server (in the first project
     *               this will not be used).
     * @return 204 if success; 404 if the uniqueId does not exist. 403 if the
     * password is incorrect. 400 otherwise.
     */
    @DELETE
    @Path("/{" + FILE_ID + "}")
    void deleteFile(@PathParam(FILE_ID) String fileId, @QueryParam(TOKEN) @DefaultValue("") String token);

    /**
     * Get the contents of the file.
     *
     * @param fileId - unique id of the file.
     * @param token  - token for accessing the file server (in the first project
     *               this will not be used).
     * @return 200 if success + contents (through redirect to the File server); 404
     * if the uniqueId does not exist. 403 if the password is incorrect. 400
     * otherwise.
     */
    @GET
    @Path("/{" + FILE_ID + "}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    byte[] getFile(@PathParam(FILE_ID) String fileId, @QueryParam(TOKEN) @DefaultValue("") String token);

    @DELETE
    @Path("/user/{" + USER_ID + "}")
    void deleteUserFiles(@PathParam(USER_ID) String userId, @QueryParam(TOKEN) @DefaultValue("") String token);

}
