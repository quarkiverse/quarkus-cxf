package io.quarkiverse.cxf.it.wss.server;

import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class CxfWssServerTestResource implements QuarkusTestResourceLifecycleManager {

    @Override
    public Map<String, String> start() {

        final String user = "cxf-user";
        final String password = "secret-password";
        return Map.of(
                "wss.username", user,
                "wss.password", password);
    }

    @Override
    public void stop() {
    }
}
