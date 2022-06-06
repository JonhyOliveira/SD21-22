package tp1.impl.servers.rest;

import org.glassfish.jersey.server.ResourceConfig;
import tp1.api.service.java.Directory;
import tp1.impl.servers.rest.util.GenericExceptionMapper;
import util.Debug;
import util.Token;

import java.util.logging.Level;
import java.util.logging.Logger;

public class DirectoryRestServer extends AbstractRestServer {

    public static final int PORT = 4567;

    private static final Logger Log = Logger.getLogger(DirectoryRestServer.class.getName());

    DirectoryRestServer() {
        super(Log, Directory.SERVICE_NAME, PORT);
    }

    @Override
    public void registerResources(ResourceConfig config, String serviceURI) {
        config.register(new DirectoryResources());
        config.register(GenericExceptionMapper.class);
//		config.register(new VersionFilter(ReplicationManager.getInstance()));
//		config.register( CustomLoggingFilter.class);
    }

    public static void main(String[] args) throws Exception {

        Debug.setLogLevel(Level.FINER, Debug.TP1);

        Token.set(args.length > 0 ? args[0] : "");

        new DirectoryRestServer().start();
    }
}