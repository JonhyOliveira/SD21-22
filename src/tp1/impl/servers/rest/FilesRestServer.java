package tp1.impl.servers.rest;

import org.glassfish.jersey.server.ResourceConfig;
import tp1.api.service.java.Files;
import tp1.impl.servers.rest.util.GenericExceptionMapper;
import util.Debug;
import util.Token;

import java.util.logging.Level;
import java.util.logging.Logger;

public class FilesRestServer extends AbstractRestServer {
    public static final int PORT = 5678;

    private static final Logger Log = Logger.getLogger(FilesRestServer.class.getName());


    FilesRestServer() {
        super(Log, Files.SERVICE_NAME, PORT);
    }

    @Override
    public void registerResources(ResourceConfig config, String serviceURI) {
        config.register(FilesResources.class);
        config.register(GenericExceptionMapper.class);
//		config.register( CustomLoggingFilter.class);
    }

    public static void main(String[] args) throws Exception {

        Debug.setLogLevel(Level.INFO, Debug.TP1);

        Token.set(args.length == 0 ? "" : args[0]);

        new FilesRestServer().start();
    }
}