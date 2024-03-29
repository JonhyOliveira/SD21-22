package tp1.impl.servers.rest;

import org.apache.kafka.common.utils.ConfigUtils;
import org.glassfish.jersey.server.ResourceConfig;
import org.pac4j.core.config.ConfigBuilder;
import tp1.api.service.java.Files;
import tp1.impl.servers.common.dropbox.util.Preserve;
import tp1.impl.servers.rest.util.CustomLoggingFilter;
import tp1.impl.servers.rest.util.GenericExceptionMapper;
import util.Debug;
import util.Token;

import java.io.ObjectInputFilter;
import java.lang.module.Configuration;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DropboxServer extends AbstractRestServer {

    public static final int PORT = 9090;
    public static final String SERVICE_NAME = Files.SERVICE_NAME;

    private static final Logger Log = Logger.getLogger(DropboxServer.class.getName());

    DropboxServer(int port) {
        super(Log, SERVICE_NAME, port);
    }

    @Override
    public void registerResources(ResourceConfig config) {
        config.register(DropboxResources.class);
        config.register(GenericExceptionMapper.class);
//      config.register(CustomLoggingFilter.class);
    }

    public static void main(String[] args) throws Exception {

        Debug.setLogLevel(Level.INFO, Debug.TP1);

        String token = "";
        boolean preserve = false;

        if (args.length >= 1) {
            preserve = !Boolean.parseBoolean(args[0]);
            if (args.length >= 2)
                token = args[1];
        }

        Token.set(token);
        Preserve.set(preserve);

        Log.info("Args: %s | Preserve: %s".formatted(args, preserve));
        new DropboxServer(PORT).start();

    }
}
