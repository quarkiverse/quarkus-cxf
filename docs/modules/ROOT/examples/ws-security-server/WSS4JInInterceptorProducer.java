/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.quarkiverse.cxf.it.wss.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.wss4j.common.ConfigurationConstants;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.arc.Unremovable;

// tag::quarkus-cxf-rt-ws-security.adoc[]
@ApplicationScoped
public class WSS4JInInterceptorProducer {

    /** Produced in CxfWssServerTestResource */
    @ConfigProperty(name = "wss.username", defaultValue = "cxf")
    String username;

    /** Produced in CxfWssServerTestResource */
    @ConfigProperty(name = "wss.password", defaultValue = "pwd")
    String password;

    @Produces
    @Unremovable
    @ApplicationScoped
    WSS4JInInterceptor wssInterceptor() {
        final CallbackHandler passwordCallback = new CallbackHandler() {
            @Override
            public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                for (Callback callback : callbacks) {
                    if (callback instanceof WSPasswordCallback) {
                        final WSPasswordCallback pc = (WSPasswordCallback) callback;
                        if (username.equals(pc.getIdentifier())) {
                            pc.setPassword(password);
                        }
                        return;
                    }
                }
            }
        };
        final Map<String, Object> props = new HashMap<>();
        props.put(ConfigurationConstants.ACTION, "UsernameToken");
        props.put(ConfigurationConstants.PASSWORD_TYPE, "PasswordText");
        props.put(ConfigurationConstants.USER, username);
        props.put(ConfigurationConstants.PW_CALLBACK_REF, passwordCallback);
        return new WSS4JInInterceptor(props);
    }
}
//end::quarkus-cxf-rt-ws-security.adoc[]
