package tp1.impl.servers.common;

import static tp1.api.service.java.Result.ErrorCode.*;
import static tp1.api.service.java.Result.error;
import static tp1.api.service.java.Result.ok;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.apache.zookeeper.CreateMode;
import tp1.api.service.java.Files;
import tp1.api.service.java.Result;
import util.Hash;
import util.IO;
import util.Token;
import util.zookeeper.Zookeeper;

public class JavaFiles implements Files {

	final static Logger Log = Logger.getLogger(JavaFiles.class.getName());

	static final String DELIMITER = "$$$";
	private static final String ROOT = "/tmp/";

	static final TokenValidation tokenVal = new TokenValidation();
	public List<Integer> tokensReceived = new ArrayList<>();
	
	public JavaFiles() {
		new File( ROOT ).mkdirs();
	}

	@Override
	public Result<byte[]> getFile(String fileId, String token) {
		if(!isTokenValid(token, "get"))
			return error(FORBIDDEN);
		fileId = fileId.replace( DELIMITER, "/");
		byte[] data = IO.read( new File( ROOT + fileId ));
		return data != null ? ok( data) : error( NOT_FOUND );
	}

	@Override
	public Result<Void> deleteFile(String fileId, String token) {
		if(!isTokenValid(token, "dl"))
			return error(FORBIDDEN);
		fileId = fileId.replace( DELIMITER, "/");
		boolean res = IO.delete( new File( ROOT + fileId ));	
		return res ? ok() : error( NOT_FOUND );
	}

	@Override
	public Result<Void> writeFile(String fileId, byte[] data, String token) {
		if(!isTokenValid(token, "wr"))
			return error(FORBIDDEN);
		fileId = fileId.replace( DELIMITER, "/");
		File file = new File(ROOT + fileId);
		file.getParentFile().mkdirs();
		IO.write( file, data);
		return ok();
	}

	@Override
	public Result<Void> deleteUserFiles(String userId, String token) {
		File file = new File(ROOT + userId);
		try {
			java.nio.file.Files.walk(file.toPath())
			.sorted(Comparator.reverseOrder())
			.map(Path::toFile)
			.forEach(File::delete);
		} catch (IOException e) {
			e.printStackTrace();
			return error(INTERNAL_ERROR);
		}
		return ok();
	}

	public static String fileId(String filename, String userId) {
		return userId + JavaFiles.DELIMITER + filename;
	}

	private boolean isTokenValid(String token, String access) {
		return tokenVal.isTokenValid(token, access);
		/*
		String[] info = token.split(Pattern.quote(DELIMITER));
		if (info.length > 1){
			long expirationDate = Long.parseLong(info[2]);
			int tokenId = Integer.parseInt(info[3]);
			String hashedToken = info[5];
			String fileId = info[0] + DELIMITER + info[1];
			String tokenToCompare = Hash.of(fileId, info[2], Token.get());
			if (tokensReceived.contains(tokenId))
				return false;
			if (!access.equals(info[4]))
				return false;
			if (System.currentTimeMillis() > expirationDate)
				return false;
			if (!tokenToCompare.equals(hashedToken))
				return false;

			tokensReceived.add(tokenId);
		}
		else
			return token.equals(Token.get());

		return true;
		 */
	}
}
