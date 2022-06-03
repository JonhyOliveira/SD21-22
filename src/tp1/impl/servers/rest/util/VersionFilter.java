package tp1.impl.servers.rest.util;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import tp1.api.service.rest.RestDirectory;
import tp1.impl.servers.common.util.ReplicationManager;

import java.io.IOException;

public class VersionFilter implements ContainerResponseFilter {

    private ReplicationManager repManager;

    public VersionFilter(ReplicationManager repManager) {
        this.repManager = repManager;
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext) throws IOException {
        containerResponseContext.getHeaders().add(RestDirectory.VERSION_HEADER, repManager.getCurrentVersion());
    }
}
