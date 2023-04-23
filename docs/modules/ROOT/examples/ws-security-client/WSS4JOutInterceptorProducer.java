package io.quarkiverse.cxf.it.wss.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.wss4j.common.ConfigurationConstants;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.arc.Unremovable;

//tag::quarkus-cxf-rt-ws-security.adoc[]
@ApplicationScoped
public class WSS4JOutInterceptorProducer {

    /** Produced in CxfWssClientTestResource */
    @ConfigProperty(name = "wss.username")
    String username;

    /** Produced in CxfWssClientTestResource */
    @ConfigProperty(name = "wss.password")
    String password;

    @Produces
    @Unremovable
    @ApplicationScoped
    WSS4JOutInterceptor wssInterceptor() {
        final CallbackHandler passwordCallback = new CallbackHandler() {
            @Override
            public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                for (Callback callback : callbacks) {
                    if (callback instanceof WSPasswordCallback) {
                        ((WSPasswordCallback) callback).setPassword(password);
                    }
                    return;
                }
            }
        };
        final Map<String, Object> props = new HashMap<>();
        props.put(ConfigurationConstants.ACTION, "UsernameToken");
        props.put(ConfigurationConstants.PASSWORD_TYPE, "PasswordText");
        props.put(ConfigurationConstants.USER, username);
        props.put(ConfigurationConstants.PW_CALLBACK_REF, passwordCallback);
        props.put(ConfigurationConstants.ADD_USERNAMETOKEN_NONCE, "true");
        props.put(ConfigurationConstants.ADD_USERNAMETOKEN_CREATED, "true");
        return new WSS4JOutInterceptor(props);
    }
}
//end::quarkus-cxf-rt-ws-security.adoc[]
