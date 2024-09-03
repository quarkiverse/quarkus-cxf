package io.quarkiverse.cxf.hc5.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;

@ApplicationScoped
@Provider
public class FactorHeaderRequestFilter implements ContainerRequestFilter {
    @Inject
    RequestScopedFactorHeader factorHeader;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        final String rawValue = requestContext.getHeaderString(RequestScopedFactorHeader.header);
        if (rawValue != null) {
            factorHeader.setHeaderValue(Integer.parseInt(rawValue));
        }
    }

    @RequestScoped
    public static class RequestScopedFactorHeader {
        public static final String header = "my-factor-header";

        public int getHeaderValue() {
            return headerValue;
        }

        public void setHeaderValue(int headerValue) {
            this.headerValue = headerValue;
        }

        private int headerValue;
    }

}
