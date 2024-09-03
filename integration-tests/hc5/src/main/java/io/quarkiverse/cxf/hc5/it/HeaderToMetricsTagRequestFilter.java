package io.quarkiverse.cxf.hc5.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;

@ApplicationScoped
@Provider
public class HeaderToMetricsTagRequestFilter implements ContainerRequestFilter {
    @Inject
    RequestScopedHeader requestScopedHeader;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        requestScopedHeader.setHeaderValue(requestContext.getHeaderString(RequestScopedHeader.header));
    }

    @RequestScoped
    public static class RequestScopedHeader {
        public static final String header = "my-header";

        public String getHeaderValue() {
            return headerValue;
        }

        public void setHeaderValue(String headerValue) {
            this.headerValue = headerValue;
        }

        private String headerValue;
    }
}
