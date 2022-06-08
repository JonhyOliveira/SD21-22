package tp1.impl.clients.common;

import tp1.api.service.java.Result;
import tp1.api.service.java.Result.ErrorCode;
import util.Sleep;

import javax.net.ssl.HttpsURLConnection;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Shared client behavior.
 * <p>
 * Used to retry an operation in a loop.
 *
 * @author smduarte
 */
public abstract class RetryClient {
    private static final Logger Log = Logger.getLogger(RetryClient.class.getName());

    protected static final int READ_TIMEOUT = 2000;
    protected static final int CONNECT_TIMEOUT = 5000;

    protected static final int RETRY_SLEEP = 100;
    protected static final int MAX_RETRIES = 3;

    {
        HttpsURLConnection.setDefaultHostnameVerifier(new InsecureHostnameVerifier());
    }

    protected <T> Result<T> reTry(Supplier<Result<T>> func) {
        return this.reTry(func, MAX_RETRIES);
    }

    protected <T> Result<T> reTry(Supplier<Result<T>> func, int numRetries) {
        for (int i = 0; i < numRetries; i++)
            try {
                return func.get();
            } catch (RuntimeException x) {
                Log.finest(">>>>>>>>Exception: " + x.getMessage() + "\n");
                Sleep.ms(RETRY_SLEEP);
            } catch (Exception x) {
                x.printStackTrace();
                Log.finest(">>>>>>>>Exception: " + x.getMessage() + "\n");
                return Result.error(ErrorCode.INTERNAL_ERROR);
            }
        return Result.error(ErrorCode.TIMEOUT);
    }
}