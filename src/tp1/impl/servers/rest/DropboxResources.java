package tp1.impl.servers.rest;

import jakarta.inject.Singleton;
import tp1.api.service.rest.RestFiles;
import tp1.impl.servers.common.DropboxImpl;
import tp1.impl.servers.common.kafka.DropboxImplKafka;

import java.util.logging.Logger;

@Singleton
public class DropboxResources extends RestResource implements RestFiles {

    private static final Logger Log = Logger.getLogger(DropboxResources.class.getName());

    private final DropboxImpl impl;

    public DropboxResources() {
        impl = new DropboxImplKafka();
    }

    @Override
    public void writeFile(String fileId, byte[] data, String token) {
        Log.info(String.format("REST writeFile: fileId = %s, data.length = %d, token = %s \n", fileId, data.length, token));

        super.resultOrThrow( impl.writeFile(fileId, data, token));
    }

    @Override
    public void deleteFile(String fileId, String token) {
        Log.info(String.format("REST deleteFile: fileId = %s, token = %s \n", fileId, token));

        super.resultOrThrow( impl.deleteFile(fileId, token));
    }

    @Override
    public byte[] getFile(String fileId, String token) {
        Log.info(String.format("REST getFile: fileId = %s,  token = %s \n", fileId, token));

        return resultOrThrow( impl.getFile(fileId, token));
    }

    @Override
    public void deleteUserFiles(String userId, String token) {
        Log.info(String.format("REST deleteUserFiles: userId = %s, token = %s \n", userId, token));

        super.resultOrThrow( impl.deleteUserFiles(userId, token));
    }
}
