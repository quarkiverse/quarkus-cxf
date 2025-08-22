package io.quarkiverse.cxf.it.extensors;

import java.util.concurrent.ExecutionException;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class ExtensorsTest {
    @Test
    void consultarRestringido() throws InterruptedException, ExecutionException {
        RestAssured.given()
                .get("/ExtensorsRest/hello/Aureliano Buendía")
                .then()
                .statusCode(200)
                .body(Matchers.is("Hello Aureliano Buendía"));
    }
}
