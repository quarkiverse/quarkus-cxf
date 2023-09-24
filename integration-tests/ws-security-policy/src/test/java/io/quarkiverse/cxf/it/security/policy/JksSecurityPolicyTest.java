package io.quarkiverse.cxf.it.security.policy;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.config.SSLConfig;

@QuarkusTest
public class JksSecurityPolicyTest extends AbstractSecurityPolicyTest {

    @Test
    void helloIp() {
        RestAssured.given()
                .config(RestAssured.config().sslConfig(new SSLConfig().with().trustStore("client-truststore.jks", "password")))
                .body("Frank")
                .post("/cxf/security-policy/helloIp")
                .then()
                /*
                 * expected to fail because the client calls the service via 127.0.0.1 which should not be allowed by the
                 * default hostname verifier
                 */
                .statusCode(500)
                .body(Matchers.containsString(
                        "The https URL hostname does not match the Common Name (CN) on the server certificate in the client's truststore"));

    }

}
