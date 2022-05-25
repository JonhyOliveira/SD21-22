package tp1.impl.servers.soap;


import java.util.logging.Level;
import java.util.logging.Logger;

import tp1.api.service.java.Files;
import util.Debug;
import util.Token;


public class FilesSoapServer extends AbstractSoapServer {

	public static final int PORT = 15678;

	private static Logger Log = Logger.getLogger(FilesSoapServer.class.getName());

	FilesSoapServer() {
		super(false, Log, Files.SERVICE_NAME, PORT, new SoapFilesWebService());
	}
	
	public static void main(String[] args) throws Exception {

		Debug.setLogLevel( Level.INFO, Debug.TP1);
		Token.set( args.length > 0 ? args[0] : "");
		
		 new FilesSoapServer().start();
	}
}
