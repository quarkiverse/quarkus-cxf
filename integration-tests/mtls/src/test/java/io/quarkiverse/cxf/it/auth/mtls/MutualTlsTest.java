package io.quarkiverse.cxf.it.auth.mtls;

import static org.hamcrest.Matchers.is;

import org.eclipse.microprofile.config.ConfigProvider;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;

@QuarkusTest
public class MutualTlsTest {
    @Test
    void mTls() {
        RestAssured.given()
                .config(restAssuredConfig())
                .body("Sam")
                .post("https://localhost:8444/cxf/mtls-rest/mTls")
                .then()
                .statusCode(200)
                .body(is("Hello Sam authenticated by mTLS!"));

    }

    @Test
    void noKeystore() {
        RestAssured.given()
                .config(restAssuredConfig())
                .body("Sam")
                .post("https://localhost:8444/cxf/mtls-rest/noKeystore")
                .then()
                .statusCode(500)
                .body(Matchers.anyOf(
                        Matchers.containsString("SSLHandshakeException: Received fatal alert: bad_certificate"),
                        Matchers.containsString("IOException: Error writing to server")));

    }

    public static RestAssuredConfig restAssuredConfig() {
        return RestAssured.config().sslConfig(new SSLConfig().with()
                .trustStore(
                        "client-truststore." + ConfigProvider.getConfig().getValue("keystore.type", String.class),
                        "client-truststore-password")
                .keyStore(
                        "client-keystore." + ConfigProvider.getConfig().getValue("keystore.type", String.class),
                        "client-keystore-password"));
    }

}
