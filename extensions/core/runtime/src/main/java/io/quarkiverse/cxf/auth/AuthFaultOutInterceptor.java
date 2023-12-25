package io.quarkiverse.cxf.auth;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;

/**
 * Set the right HTTP status code for the response based on the kind of {@link SecurityException} being thrown.
 */
public class AuthFaultOutInterceptor extends AbstractSoapInterceptor {

    public AuthFaultOutInterceptor() {
        super(Phase.POST_LOGICAL);
    }

    @Override
    public void handleMessage(SoapMessage message) throws Fault {
        final Exception e = message.getContent(Exception.class);
        if (e instanceof Fault) {
            final Throwable securityException = rootCause(e);
            if (securityException instanceof UnauthorizedException) {
                ((Fault) e).setStatusCode(401);
            } else if (securityException instanceof ForbiddenException) {
                ((Fault) e).setStatusCode(403);
            }
        }
    }

    private static Throwable rootCause(Exception e) {
        Throwable result = e;
        while (result.getCause() != null) {
            result = result.getCause();
        }
        return result;
    }

}
