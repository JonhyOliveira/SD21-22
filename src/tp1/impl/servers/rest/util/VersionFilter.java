package tp1.impl.servers.rest.util;

import com.google.gson.Gson;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import tp1.api.service.rest.RestDirectory;
import tp1.impl.servers.common.JavaDirectorySynchronizer;
import tp1.impl.servers.common.replication.ReplicationManager;
import util.Json;

import java.io.IOException;
import java.util.logging.Logger;

@Provider
public record VersionFilter(ReplicationManager manager) implements ContainerResponseFilter, ContainerRequestFilter {

    private static final Logger Log = Logger.getLogger(VersionFilter.class.getName());
    private static Gson json = Json.getInstance();

    @Override
    public void filter(ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext) throws IOException {
        Log.info("Next version should be >= %s".formatted(manager.version().toString()));
        containerResponseContext.getHeaders().add(RestDirectory.VERSION_HEADER, Json.getInstance().toJson(manager.version()));
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        var versionHeader = containerRequestContext.getHeaderString(RestDirectory.VERSION_HEADER);
        Log.info("Version: %s %s".formatted(versionHeader, versionHeader == null));

        if (versionHeader == null) // the client probably wants do  the operation in the current state
            containerRequestContext.getHeaders().add(RestDirectory.VERSION_HEADER, Json.getInstance().toJson(manager.version()));

    }
}
