package io.quarkiverse.cxf.hc5.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;

@ApplicationScoped
@Provider
public class MyContainerRequestFilter implements ContainerRequestFilter {
    @Inject
    RequestScopedHeader requestScopedHeader;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        requestScopedHeader.setHeaderValue(requestContext.getHeaderString(RequestScopedHeader.header));
    }
}
