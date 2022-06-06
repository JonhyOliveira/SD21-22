package tp1.impl.servers.rest;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import tp1.impl.discovery.Discovery;
import tp1.impl.servers.common.AbstractServer;
import util.IP;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

public abstract class AbstractRestServer extends AbstractServer {

    protected static String SERVER_BASE_URI = "https://%s:%s/rest";

    protected AbstractRestServer(Logger log, String service, int port) {
        super(log, service, port);
    }


    protected void start() throws NoSuchAlgorithmException {
        String ip = IP.hostAddress();
        String serverURI = String.format(SERVER_BASE_URI, ip, port);

        ResourceConfig config = new ResourceConfig();

        registerResources(config, serverURI);

        JdkHttpServerFactory.createHttpServer(URI.create(serverURI.replace(ip, INETADDR_ANY)), config, SSLContext.getDefault());

        Log.info(String.format("%s Server ready @ %s\n", service, serverURI));

        Discovery.getInstance().announce(service, serverURI);
    }

    public abstract void registerResources(ResourceConfig config, String serviceURI);
}
