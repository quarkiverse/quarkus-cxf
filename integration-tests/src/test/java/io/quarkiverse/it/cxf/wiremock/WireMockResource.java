package io.quarkiverse.it.cxf.wiremock;

import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class WireMockResource implements QuarkusTestResourceLifecycleManager {

    WireMockServer wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort().httpsPort(-1));

    @Override
    public Map<String, String> start() {
        wireMockServer.start();

        // overwrites the config from application.properties
        // so your test client sends requests always to this URL
        return Map.of("quarkus.cxf.client.\"greeting\".client-endpoint-url",
                "http://localhost:" + wireMockServer.port() + "/soap/greeting");
    }

    @Override
    public synchronized void stop() {
        if (wireMockServer != null) {
            wireMockServer.stop();
            wireMockServer = null;
        }
    }

    @Override
    public void inject(TestInjector testInjector) {
        testInjector.injectIntoFields(wireMockServer,
                new TestInjector.AnnotatedAndMatchesType(InjectWireMock.class, WireMockServer.class));
    }

}
