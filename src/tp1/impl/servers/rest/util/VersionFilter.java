package tp1.impl.servers.rest.util;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import tp1.api.service.rest.RestDirectory;
import tp1.impl.servers.common.ReplicationManager;
import tp1.impl.servers.rest.DirectoryResources;

import java.io.IOException;
import java.util.logging.Logger;

public record VersionFilter(ReplicationManager repManager) implements ContainerResponseFilter, ContainerRequestFilter {

    private static Logger Log = Logger.getLogger(VersionFilter.class.getName());

    @Override
    public void filter(ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext) throws IOException {
        containerResponseContext.getHeaders().add(RestDirectory.VERSION_HEADER, repManager.getCurrentVersion());
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        if (containerRequestContext.getHeaders().get(RestDirectory.VERSION_HEADER) == null)
            containerRequestContext.getHeaders().add(RestDirectory.VERSION_HEADER, "0");

        Log.info("Requested version: %s".formatted(containerRequestContext.getHeaders().get(RestDirectory.VERSION_HEADER)));
    }
}
