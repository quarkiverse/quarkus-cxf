package io.quarkiverse.cxf.it.security.policy;

import org.eclipse.microprofile.config.ConfigProvider;

import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;

public class PolicyTestUtils {

    public static RestAssuredConfig restAssuredConfig() {
        return RestAssured.config().sslConfig(new SSLConfig().with().trustStore(
                "client-truststore." + ConfigProvider.getConfig().getValue("keystore.type", String.class),
                "client-truststore-password"));
    }

}
