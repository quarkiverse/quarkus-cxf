package io.quarkiverse.cxf.it.wss.client;

import java.io.IOException;
import java.io.InputStream;

import org.apache.wss4j.common.WSS4JConstants;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class SignatureValidatorTest {

    @Test
    public void saml1() throws Exception {
        assertSaml("data/saml1-request.xml", WSS4JConstants.SAML_NS);
    }

    @Test
    public void saml2() throws Exception {
        assertSaml("data/saml2-request.xml", WSS4JConstants.SAML2_NS);
    }

    static void assertSaml(String message, String samlNamespace) throws IOException {
        try (InputStream in = SignatureValidatorTest.class.getClassLoader().getResourceAsStream(message)) {
            RestAssured.given()
                    .body(in)
                    .queryParam("samlNamespace", samlNamespace)
                    .post("/cxf/signature-validator/validate")
                    .then()
                    .statusCode(200)
                    .body(CoreMatchers.is("true"));
        }
    }

}
