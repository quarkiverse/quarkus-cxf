package io.quarkiverse.cxf.it.ws.trust.common;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.wss4j.common.ext.WSPasswordCallback;

public class PasswordCallbackHandler implements CallbackHandler {

    private Map<String, String> passwords = new HashMap<String, String>();

    public PasswordCallbackHandler(Map<String, String> initMap) {
        passwords.putAll(initMap);
    }

    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (int i = 0; i < callbacks.length; i++) {
            final Callback c = callbacks[i];
            if (c != null && c instanceof WSPasswordCallback) {
                final WSPasswordCallback pc = (WSPasswordCallback) c;

                String pass = passwords.get(pc.getIdentifier());
                if (pass != null) {
                    pc.setPassword(pass);
                    return;
                }
            }
        }
    }

}
