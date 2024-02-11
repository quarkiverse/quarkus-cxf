package io.quarkiverse.cxf.it.wss.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.wss4j.common.ConfigurationConstants;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.assertj.core.api.Assertions;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import io.quarkiverse.cxf.test.QuarkusCxfClientTestUtil;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(CxfWssServerTestResource.class)
public class CxfWssServerTest {

    @Test
    void anonymous() throws IOException {
        final WssRounderService client = QuarkusCxfClientTestUtil.getClient(WssRounderService.class, "/soap/rounder");
        /* Make sure that it fails properly when called without a password */
        Assertions.assertThatExceptionOfType(jakarta.xml.ws.soap.SOAPFaultException.class)
                .isThrownBy(() -> client.round(2.8))
                .withMessage(
                        "A security error was encountered when verifying the message");

    }

    @Test
    void usernameToken() throws IOException {

        final Config config = ConfigProvider.getConfig();
        final String username = config.getValue("wss.username", String.class);
        final String password = config.getValue("wss.password", String.class);

        final WssRounderService client = QuarkusCxfClientTestUtil.getClient(WssRounderService.class, "/soap/rounder");

        final CallbackHandler passwordCallback = new CallbackHandler() {
            @Override
            public void handle(Callback[] callbacks) throws UnsupportedCallbackException {
                for (Callback callback : callbacks) {
                    if (callback instanceof WSPasswordCallback) {
                        ((WSPasswordCallback) callback).setPassword(password);
                        break;
                    }
                }
            }
        };

        final Map<String, Object> props = new HashMap<>();
        props.put(ConfigurationConstants.ACTION, "UsernameToken");
        props.put(ConfigurationConstants.PASSWORD_TYPE, "PasswordText");
        props.put(ConfigurationConstants.USER, username);
        props.put(ConfigurationConstants.PW_CALLBACK_REF, passwordCallback);

        Client clientProxy = ClientProxy.getClient(client);
        clientProxy.getOutInterceptors().add(new WSS4JOutInterceptor(props));

        Assertions.assertThat(client.round(2.1)).isEqualTo(2);

    }

}
