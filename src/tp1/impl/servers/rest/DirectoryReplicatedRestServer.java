package tp1.impl.servers.rest;

import org.glassfish.jersey.server.ResourceConfig;
import tp1.impl.servers.common.ReplicationManager;
import tp1.impl.servers.rest.util.GenericExceptionMapper;
import tp1.impl.servers.rest.util.VersionFilter;
import util.Debug;
import util.Token;

import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;

public class DirectoryReplicatedRestServer extends DirectoryRestServer {

    DirectoryReplicatedRestServer() {
        super();
    }

    @Override
    public void registerResources(ResourceConfig config) {
        ReplicationManager repManager = ReplicationManager.getInstance();

        config.register( new DirectoryReplicatedResources(repManager) );
        config.register( GenericExceptionMapper.class );
        config.register( new VersionFilter(repManager) );
    }

    public static void main(String[] args) throws NoSuchAlgorithmException {
        Debug.setLogLevel( Level.FINER, Debug.TP1);

        Token.set( args.length > 0 ? args[0] : "");

        new DirectoryReplicatedRestServer().start();
    }
}