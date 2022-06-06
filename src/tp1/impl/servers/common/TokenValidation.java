package tp1.impl.servers.common;

import org.apache.kafka.common.utils.Java;
import util.Hash;
import util.Token;

import java.util.List;
import java.util.regex.Pattern;

public class TokenValidation {

    JavaFiles files = new JavaFiles();
    //private static List<Integer> tokensReceived;

    public TokenValidation() {}

    public boolean isTokenValid(String token, String access) {
        String[] info = token.split(Pattern.quote(JavaFiles.DELIMITER));
        if (info.length > 1){
            long expirationDate = Long.parseLong(info[2]);
            int tokenId = Integer.parseInt(info[3]);
            String hashedToken = info[5];
            String fileId = info[0] + JavaFiles.DELIMITER + info[1];
            String tokenToCompare = Hash.of(fileId, info[2], Token.get());
            if (files.tokensReceived.contains(tokenId))
                return false;
            if (!access.equals(info[4]))
                return false;
            if (System.currentTimeMillis() > expirationDate)
                return false;
            if (!tokenToCompare.equals(hashedToken))
                return false;

            files.tokensReceived.add(tokenId);
        }
        else {
            System.out.println("SECOND");
            return token.equals(Token.get());
        }

        return true;
    }
}
