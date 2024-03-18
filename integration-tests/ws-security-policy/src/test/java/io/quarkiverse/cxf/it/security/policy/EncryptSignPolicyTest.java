package io.quarkiverse.cxf.it.security.policy;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;

@QuarkusTest
public class EncryptSignPolicyTest extends AbstractFipsAwareTest {

    @Test
    void helloEncryptSign() throws IOException {
        failFipsInNative();

        ValidatableResponse response = RestAssured.given()
                .config(PolicyTestUtils.restAssuredConfig())
                .body("Dolly")
                .post("/cxf/security-policy/helloEncryptSign")
                .then();

        if (isFipsEnabled()) {
            response.statusCode(500)
                    .body(containsString("Cannot find any provider supporting RSA/ECB/OAEPWithSHA1AndMGF1Padding"));
        } else {
            response.statusCode(200)
                    .body(is("Hello Dolly from EncryptSign!"));
        }
    }
}
