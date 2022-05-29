package tp1.impl.servers.rest;

import org.glassfish.jersey.server.ResourceConfig;
import tp1.api.service.java.Files;
import tp1.impl.servers.rest.util.CustomLoggingFilter;
import tp1.impl.servers.rest.util.GenericExceptionMapper;
import util.Debug;
import util.Token;

import java.util.logging.Level;
import java.util.logging.Logger;

public class DropboxServer extends AbstractRestServer {

    public static final int PORT = 9090;
    public static final String SERVICE_NAME = Files.SERVICE_NAME;

    private static final Logger Log = Logger.getLogger(DropboxServer.class.getName());

    public static boolean PRESERVE;

    DropboxServer(int port) {
        super(Log, SERVICE_NAME, port);
    }

    @Override
    public void registerResources(ResourceConfig config) {
        config.register(DropboxResources.class);
        config.register(GenericExceptionMapper.class);
        config.register(CustomLoggingFilter.class);
    }

    public static void main(String[] args) throws Exception {

        Debug.setLogLevel(Level.INFO, Debug.TP1);

        String token = "";
        boolean preserve = false;

        if (args.length >= 1) {
            preserve = Boolean.parseBoolean(args[0]);
            if (args.length >= 2)
                token = args[1];
        }

        Token.set(token);
        PRESERVE = preserve;

        new DropboxServer(PORT).start();

    }
}
