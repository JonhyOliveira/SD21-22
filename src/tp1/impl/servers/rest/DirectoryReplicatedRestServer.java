package tp1.impl.servers.rest;

import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import tp1.impl.servers.common.JavaDirectorySynchronizer;
import tp1.impl.servers.rest.util.GenericExceptionMapper;
import tp1.impl.servers.rest.util.VersionFilter;
import util.Debug;
import util.Token;

import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;

public class DirectoryReplicatedRestServer extends DirectoryRestServer {

    private static final String ELECTION_NAME = "directoryLeader";

    DirectoryReplicatedRestServer() {
        super();
    }

    @Override
    public void registerResources(ResourceConfig config) {
        JavaDirectorySynchronizer synchronizer = new JavaDirectorySynchronizer();

        config.register(new DirectoryReplicatedResources(synchronizer));
        config.register(GenericExceptionMapper.class);
        config.register(new VersionFilter(synchronizer));
    }

    public static void main(String[] args) throws NoSuchAlgorithmException {
        Debug.setLogLevel(Level.ALL, Debug.TP1);

        Token.set(args.length > 0 ? args[0] : "");

        new DirectoryReplicatedRestServer().start();
    }
}