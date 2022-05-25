package tp1.impl.servers.rest;

import jakarta.inject.Singleton;
import tp1.api.service.rest.RestFiles;
import tp1.impl.servers.common.DropboxImpl;

import java.util.logging.Logger;

@Singleton
public class DropboxResources extends FilesResources implements RestFiles {

    protected static Logger Log = Logger.getLogger(DropboxResources.class.getName());

    public DropboxResources() {
        impl = new DropboxImpl();
    }

}
