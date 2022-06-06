package tp1.impl.servers.rest.util;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import tp1.api.service.rest.RestDirectory;
import tp1.impl.servers.common.JavaDirectorySynchronizer;
import tp1.impl.servers.common.replication.ReplicationManager;

import java.io.IOException;
import java.util.logging.Logger;

@Provider
public record VersionFilter(ReplicationManager manager) implements ContainerResponseFilter {

    private static final Logger Log = Logger.getLogger(VersionFilter.class.getName());

    @Override
    public void filter(ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext) throws IOException {
        Log.info("Next version should be >= %s".formatted(manager.version().toString()));
        containerResponseContext.getHeaders().add(RestDirectory.VERSION_HEADER, manager.version().toString());
    }

}
