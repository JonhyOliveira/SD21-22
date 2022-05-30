package tp1.impl.servers.soap;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import jakarta.xml.ws.Endpoint;
import tp1.impl.clients.common.InsecureHostnameVerifier;
import tp1.impl.discovery.Discovery;
import tp1.impl.servers.common.AbstractServer;
import util.IP;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

public class AbstractSoapServer extends AbstractServer{
	private static String SERVER_BASE_URI = "https://%s:%s/soap";

	final Object implementor;
	
	protected AbstractSoapServer( boolean enableSoapDebug, Logger log, String service, int port, Object implementor) {
		super( log, service, port);
		this.implementor = implementor;
		
		if(enableSoapDebug ) {
			System.setProperty("com.sun.xml.ws.transport.http.client.HttpTransportPipe.dump", "true");
			System.setProperty("com.sun.xml.internal.ws.transport.http.client.HttpTransportPipe.dump", "true");
			System.setProperty("com.sun.xml.ws.transport.http.HttpAdapter.dump", "true");
			System.setProperty("com.sun.xml.internal.ws.transport.http.HttpAdapter.dump", "true");
		}
	}
	
	protected void start() {
		var ip = IP.hostAddress();

		try {
			var server = HttpsServer.create(new InetSocketAddress(ip, port), 0);

			HttpsURLConnection.setDefaultHostnameVerifier(new InsecureHostnameVerifier());

			server.setExecutor(Executors.newCachedThreadPool());
			server.setHttpsConfigurator(new HttpsConfigurator(SSLContext.getDefault()));

			var endpoint = Endpoint.create(implementor);
			endpoint.publish(server.createContext("/soap"));

			server.start();
		}catch (Exception e ) {
			e.printStackTrace();
		}

		var serverURI = String.format(SERVER_BASE_URI, ip, port);

		Discovery.getInstance().announce(service, serverURI);

		Log.info(String.format("%s Soap Server ready @ %s\n", service, serverURI));
	}
}
