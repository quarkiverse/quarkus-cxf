package io.quarkiverse.cxf.it.security.policy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.awaitility.Awaitility;

import io.restassured.RestAssured;
import io.restassured.config.SSLConfig;

public class PolicyTestUtils {

    static List<String> drainMessages(String endpoint, int count) {
        List<String> messages = new ArrayList<>();

        if (count < 0) {
            final String body = RestAssured.given()
                    .config(RestAssured.config()
                            .sslConfig(new SSLConfig().with().trustStore("client-truststore.p12", "password")))
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
                                        .config(RestAssured.config()
                                                .sslConfig(
                                                        new SSLConfig().with().trustStore("client-truststore.p12", "password")))
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

}
