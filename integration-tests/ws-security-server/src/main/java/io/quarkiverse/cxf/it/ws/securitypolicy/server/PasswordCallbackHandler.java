package io.quarkiverse.cxf.it.ws.securitypolicy.server;

import java.io.IOException;
import java.util.Map;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.wss4j.common.ext.WSPasswordCallback;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(methods = false, fields = false)
public class PasswordCallbackHandler implements CallbackHandler {

    private final Map<String, String> passwords;

    public PasswordCallbackHandler() {
        this.passwords = Map.of(
                "alice", "password",
                "bob", "password");
    }

    /**
     * It attempts to get the password from the private alias/passwords map.
     */
    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (int i = 0; i < callbacks.length; i++) {
            WSPasswordCallback pc = (WSPasswordCallback) callbacks[i];

            String pass = passwords.get(pc.getIdentifier());
            if (pass != null) {
                pc.setPassword(pass);
                return;
            }
        }
    }

}
