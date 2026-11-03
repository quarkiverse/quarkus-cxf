package io.quarkiverse.cxf.it.auth.oidc;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.keycloak.client.KeycloakTestClient;
import io.restassured.specification.RequestSpecification;

@QuarkusTest
public class HelloServiceTest {

    private final KeycloakTestClient keycloak = new KeycloakTestClient();

    @ParameterizedTest
    @CsvSource({
            "hello, Hello,",
            "hallo, Hallo,",
            "hello, Hello, alice",
            "hallo, Hallo, alice",
            "ciao, Ciao, alice",
            "hoi, Hoi, alice",
            "hello, Hello, bob",
            "hallo, Hallo, bob",
            "hoi, Hoi, bob"
    })
    void permittedInvocations(String operation, String greeting, String user) {
        RequestSpecification request = soapRequest(operation);
        if (user != null) {
            request.auth().oauth2(keycloak.getAccessToken(user));
        }
        request.post("/services/hello")
                .then()
                .statusCode(200)
                .body("Envelope.Body." + operation + "Response.return", equalTo(greeting + " World!"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "ciao", "hoi" })
    void requiresAuthentication(String operation) {
        soapRequest(operation)
                .post("/services/hello")
                .then()
                .statusCode(401);
    }

    @Test
    void ciaoRequiresItalianRole() {
        soapRequest("ciao")
                .auth().oauth2(keycloak.getAccessToken("bob"))
                .post("/services/hello")
                .then()
                .statusCode(403);
    }

    @ParameterizedTest
    @ValueSource(strings = { "hello", "hallo", "ciao", "hoi" })
    void invalidTokenIsRejected(String operation) {
        soapRequest(operation)
                .auth().oauth2("invalid-token")
                .post("/services/hello")
                .then()
                .statusCode(401);
    }

    private RequestSpecification soapRequest(String operation) {
        return given()
                .contentType("text/xml; charset=UTF-8")
                .body("""
                        <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
                                xmlns:hello="http://oidc.auth.it.cxf.quarkiverse.io/">
                            <soap:Body>
                                <hello:%1$s><name>World</name></hello:%1$s>
                            </soap:Body>
                        </soap:Envelope>
                        """.formatted(operation));
    }
}
