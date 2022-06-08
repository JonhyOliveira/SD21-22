package tp1.impl.servers.common.replication;

import tp1.api.service.java.Result;
import tp1.impl.servers.common.JavaDirectorySynchronizer;
import util.Json;
import util.Token;

import java.net.URI;

import static tp1.impl.clients.Clients.DirectoryClients;

public class FollowerDeltaApplier extends SequencedDeltaApplier {

    private final URI serviceURI;

    public URI serviceURI() {
        return serviceURI;
    }

    public FollowerDeltaApplier(URI serviceURI) {
        super();
        this.serviceURI = serviceURI;
    }

    @Override
    protected boolean apply(SequencedFileDelta delta) {
        var version = Json.getInstance().toJson(delta.sequencingVersion());
        Log.fine("Sending delta %s to %s".formatted(version, serviceURI));
        var res = DirectoryClients.get(serviceURI).applyDelta(version, Token.get(), delta.deltaToExecute());
        if (res.isOK()) {
            Log.finer("Sent delta %s to %s".formatted(version, serviceURI));
            delta.done().countDown();
        }
        else if (!res.error().equals(Result.ErrorCode.TIMEOUT)) {
            throw new IllegalStateException(("""
                            Apply delta on follower %s got error code: %s
                            Reason: %s
                            D: What do?""")
                    .formatted(serviceURI, res.error(), res.errorValue()));
        }
        else { // Down or overloaded, don't continue
            Log.fine("Got Timeout from %s".formatted(serviceURI));
            return false;
        }

        return true;
    }

}
