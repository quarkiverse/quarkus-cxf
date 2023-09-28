package io.quarkiverse.cxf.it.security.policy;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
@Named("usernameTokenPasswordCallback")
public class UsernameTokenPasswordCallback implements CallbackHandler {

    @ConfigProperty(name = "wss.password")
    String password;

    @ConfigProperty(name = "wss.user")
    String user;

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        if (callbacks.length < 1) {
            throw new IllegalStateException("Expected a " + WSPasswordCallback.class.getName()
                    + " at possition 0 of callbacks. Got array of length " + callbacks.length);
        }
        if (!(callbacks[0] instanceof WSPasswordCallback)) {
            throw new IllegalStateException(
                    "Expected a " + WSPasswordCallback.class.getName() + " at possition 0 of callbacks. Got an instance of "
                            + callbacks[0].getClass().getName() + " at possition 0");
        }
        final WSPasswordCallback pc = (WSPasswordCallback) callbacks[0];
        if (user.equals(pc.getIdentifier())) {
            pc.setPassword(password);
        } else {
            throw new IllegalStateException("Unexpected user " + user);
        }
    }

}
