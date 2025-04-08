package io.quarkiverse.cxf.metrics.it.async;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;

import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.jboss.eap.quickstarts.wscalculator.calculator.Add;

@Singleton
@Named("multiplyingAddInterceptor")
public class MultiplyingAddInterceptor extends AbstractPhaseInterceptor<Message> {

    @Inject
    RequestScopedFactorHeader factorHeader;

    public MultiplyingAddInterceptor() {
        super(Phase.USER_LOGICAL);
    }

    @Override
    public void handleMessage(final Message message) {
        final Add add = (Add) message.getContent(List.class).get(0);
        add.setArg0(add.getArg0() * factorHeader.getHeaderValue());
        add.setArg1(add.getArg1() * factorHeader.getHeaderValue());
    }

    @ApplicationScoped
    @Provider
    public static class FactorHeaderRequestFilter implements ContainerRequestFilter {
        @Inject
        RequestScopedFactorHeader factorHeader;

        @Override
        public void filter(ContainerRequestContext requestContext) {
            final String rawValue = requestContext.getHeaderString(RequestScopedFactorHeader.header);
            if (rawValue != null) {
                factorHeader.setHeaderValue(Integer.parseInt(rawValue));
            }
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
