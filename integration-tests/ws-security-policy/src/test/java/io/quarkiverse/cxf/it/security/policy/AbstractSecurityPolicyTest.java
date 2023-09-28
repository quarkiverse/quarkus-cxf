package io.quarkiverse.cxf.it.security.policy;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.restassured.RestAssured;
import io.restassured.config.SSLConfig;

public abstract class AbstractSecurityPolicyTest {

    @Test
    void hello() {
        RestAssured.given()
                .config(RestAssured.config().sslConfig(new SSLConfig().with().trustStore("client-truststore.jks", "password")))
                .body("Frank")
                .post("/cxf/security-policy/hello")
                .then()
                .statusCode(200)
                .body(is("Hello Frank!"));
    }

    @Test
    void helloHttps() {
        RestAssured.given()
                .config(RestAssured.config().sslConfig(new SSLConfig().with().trustStore("client-truststore.jks", "password")))
                .body("Frank")
                .post("/cxf/security-policy/helloHttps")
                .then()
                .statusCode(200)
                .body(is("Hello Frank from HTTPS!"));
    }

    @Test
    void helloHttp() {
        RestAssured.given()
                .config(RestAssured.config().sslConfig(new SSLConfig().with().trustStore("client-truststore.jks", "password")))
                .body("Frank")
                .post("/cxf/security-policy/helloHttp")
                .then()
                .statusCode(500)
                .body(containsString("TransportBinding: TLS is not enabled"));
    }

}
