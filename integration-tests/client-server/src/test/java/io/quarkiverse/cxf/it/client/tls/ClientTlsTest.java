package io.quarkiverse.cxf.it.client.tls;

import org.hamcrest.Matchers;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
@QuarkusTestResource(ClientTlsTestResource.class)
class ClientTlsTest {

    @ParameterizedTest
    @ValueSource(strings = { //
            "sync/vertxClient", //
            "sync/urlConnectionClient", //
            "sync/httpClient"
    })
    void redirect(String endpoint) {
        RestAssured
                .given()
                .body("Jane")
                .post("/ClientTlsRest/" + endpoint)
                .then()
                .statusCode(200)
                .body(Matchers.containsString("Hello from Client Tls, Jane"));
    }
}
