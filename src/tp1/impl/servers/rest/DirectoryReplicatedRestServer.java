package tp1.impl.servers.rest;

import org.glassfish.jersey.server.ResourceConfig;
import tp1.api.service.java.Directory;
import tp1.impl.servers.common.JavaDirectorySynchronizer;
import tp1.impl.servers.common.replication.ReplicationManager;
import tp1.impl.servers.common.replication.Version;
import tp1.impl.servers.rest.util.GenericExceptionMapper;
import tp1.impl.servers.rest.util.VersionFilter;
import util.Debug;
import util.Token;

import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DirectoryReplicatedRestServer extends AbstractRestServer {

    public static final int PORT = 4567;

    private static final java.util.logging.Logger Log = Logger.getLogger(DirectoryRestServer.class.getName());

    DirectoryReplicatedRestServer() {
        super(Log, Directory.SERVICE_NAME, PORT);
    }

    @Override
    public void registerResources(ResourceConfig config, String serviceURI) {
        ReplicationManager replicationManager = new ReplicationManager(serviceURI);
        replicationManager.setVersion(Version.ZERO_VERSION);

        config.register(new DirectoryReplicatedResources(replicationManager));
        config.register(GenericExceptionMapper.class);
		config.register(new VersionFilter(replicationManager));
//		config.register( CustomLoggingFilter.class);
    }

    public static void main(String[] args) throws Exception {

        Debug.setLogLevel(Level.FINER, Debug.TP1);

        Token.set(args.length > 0 ? args[0] : "");

        new DirectoryReplicatedRestServer().start();
    }
}