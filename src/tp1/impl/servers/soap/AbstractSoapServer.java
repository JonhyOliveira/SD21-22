package tp1.impl.servers.soap;

import java.util.logging.Logger;

import jakarta.xml.ws.Endpoint;
import tp1.impl.discovery.Discovery;
import tp1.impl.servers.common.AbstractServer;
import util.IP;

public class AbstractSoapServer extends AbstractServer{
	private static String SERVER_BASE_URI = "http://%s:%s/soap";

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
		var serverURI = String.format(SERVER_BASE_URI, ip, port);

		Endpoint.publish(serverURI.replace(ip, INETADDR_ANY), implementor );

		Discovery.getInstance().announce(service, serverURI);

		Log.info(String.format("%s Soap Server ready @ %s\n", service, serverURI));
	}
}
