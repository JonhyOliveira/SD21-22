package tp1.impl.servers.rest.util;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import tp1.api.service.rest.RestDirectory;
import tp1.impl.servers.common.JavaDirectorySynchronizer;

import java.io.IOException;
import java.util.logging.Logger;

public record VersionFilter(
        JavaDirectorySynchronizer synchronizer) implements ContainerResponseFilter, ContainerRequestFilter {

    private static final Logger Log = Logger.getLogger(VersionFilter.class.getName());

    @Override
    public void filter(ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext) throws IOException {
        Log.info("Next version should be: %s".formatted(synchronizer.getCurrentVersion().toString()));
        containerResponseContext.getHeaders().add(RestDirectory.VERSION_HEADER, synchronizer.getCurrentVersion().toString());
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        if (containerRequestContext.getHeaders().get(RestDirectory.VERSION_HEADER) == null)
            containerRequestContext.getHeaders().add(RestDirectory.VERSION_HEADER, synchronizer.getCurrentVersion().toString());

        Log.info("Requested version: %s".formatted(containerRequestContext.getHeaders().get(RestDirectory.VERSION_HEADER)));
    }
}
