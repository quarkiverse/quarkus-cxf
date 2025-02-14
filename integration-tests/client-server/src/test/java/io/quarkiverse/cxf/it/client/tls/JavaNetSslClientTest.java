package io.quarkiverse.cxf.it.client.tls;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
@QuarkusTestResource(JavaNetSslClientTestResource.class)
class JavaNetSslClientTest {
    @Test
    void javaNetSslClient() {
        RestAssured
                .given()
                .body("Jane")
                .post("/JavaNetSslClient/sync/javaNetSslClient")
                .then()
                .statusCode(200)
                .body(Matchers.is("Hello Jane, Content-Type: text/xml; charset=UTF-8"));
    }
}
