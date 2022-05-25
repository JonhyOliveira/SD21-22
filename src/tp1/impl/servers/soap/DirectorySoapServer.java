package tp1.impl.servers.soap;


import java.util.logging.Level;
import java.util.logging.Logger;

import tp1.api.service.java.Directory;
import util.Debug;
import util.Token;


public class DirectorySoapServer extends AbstractSoapServer {


	public static final int PORT = 14567;
	private static Logger Log = Logger.getLogger(DirectorySoapServer.class.getName());

	protected DirectorySoapServer() {
		super(false, Log, Directory.SERVICE_NAME, PORT, new SoapDirectoryWebService());
	}

	public static void main(String[] args) throws Exception {

		Debug.setLogLevel( Level.INFO, Debug.TP1);
		Token.set( args.length > 0 ? args[0] : "");
		
		new DirectorySoapServer().start();
	}
}
