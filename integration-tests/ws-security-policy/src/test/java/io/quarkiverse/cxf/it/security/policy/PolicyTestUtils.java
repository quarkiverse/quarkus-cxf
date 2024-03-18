package io.quarkiverse.cxf.it.security.policy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.awaitility.Awaitility;
import org.eclipse.microprofile.config.ConfigProvider;

import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;

public class PolicyTestUtils {

    static List<String> drainMessages(String endpoint, int count) {
        List<String> messages = new ArrayList<>();

        if (count < 0) {
            final String body = RestAssured.given()
                    .config(restAssuredConfig())
                    .get("/cxf/security-policy/" + endpoint)
                    .then()
                    .statusCode(200)
                    .extract().body().asString();
            Stream.of(body.split("\\Q|||\\E")).forEach(messages::add);
        } else {
            Awaitility.waitAtMost(30, TimeUnit.SECONDS)
                    .until(
                            () -> {
                                final String body = RestAssured.given()
                                        .config(restAssuredConfig())
                                        .get("/cxf/security-policy/" + endpoint)
                                        .then()
                                        .statusCode(200)
                                        .extract().body().asString();
                                Stream.of(body.split("\\Q|||\\E")).forEach(messages::add);
                                return messages.size() >= count;
                            });
        }
        return messages;

    }

    public static RestAssuredConfig restAssuredConfig() {
        return RestAssured.config().sslConfig(new SSLConfig().with().trustStore(
                "client-truststore." + ConfigProvider.getConfig().getValue("keystore.type", String.class),
                "client-truststore-password"));
    }

    public static boolean isFipsLocalEnabled() throws IOException {
        return java.security.Security.getProvider("SunPKCS11-NSS-FIPS") != null;
    }
}
