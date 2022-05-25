package tp1.impl.servers.soap;


import java.util.logging.Level;
import java.util.logging.Logger;

import tp1.api.service.java.Users;
import util.Debug;
import util.Token;


public class UsersSoapServer extends AbstractSoapServer{

	public static final int PORT = 13456;
	private static Logger Log = Logger.getLogger(UsersSoapServer.class.getName());

	UsersSoapServer() {
		super(false, Log, Users.SERVICE_NAME, PORT, new SoapUsersWebService());
	}
	
	public static void main(String[] args) throws Exception {

		Debug.setLogLevel( Level.INFO, Debug.TP1);
		Token.set( args.length > 0 ? args[0] : "");
		
		new UsersSoapServer().start();
	}

}
