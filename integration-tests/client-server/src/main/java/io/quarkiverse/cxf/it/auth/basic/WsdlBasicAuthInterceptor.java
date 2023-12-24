package io.quarkiverse.cxf.it.auth.basic;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.transport.http.AbstractHTTPDestination;

@ApplicationScoped
@Named("wsdlBasicAuthInterceptor")
public class WsdlBasicAuthInterceptor extends AbstractPhaseInterceptor<SoapMessage> {

    public WsdlBasicAuthInterceptor() {
        super(Phase.RECEIVE);
    }

    @Override
    public void handleMessage(SoapMessage message) throws Fault {

        final HttpServletRequest req = (HttpServletRequest) message.getExchange().getInMessage()
                .get(AbstractHTTPDestination.HTTP_REQUEST);
        if ("GET".equals(req.getMethod())) {
            /* WSDL is the only thing served through GET */
            try {
                auth();
            } catch (io.quarkus.security.UnauthorizedException e) {
                handle40x(message, HttpServletResponse.SC_UNAUTHORIZED);
            } catch (io.quarkus.security.ForbiddenException e) {
                handle40x(message, HttpServletResponse.SC_FORBIDDEN);
            }
        }
    }

    private void handle40x(SoapMessage message, int code) {
        HttpServletResponse response = (HttpServletResponse) message.getExchange().getInMessage()
                .get(AbstractHTTPDestination.HTTP_RESPONSE);
        response.setStatus(code);
        message.getInterceptorChain().abort();
    }

    @RolesAllowed("app-user")
    void auth() {

    }
}
