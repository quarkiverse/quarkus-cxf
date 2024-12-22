package io.quarkiverse.cxf.it.soap12;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
class Soap12Test {

    @Test
    void soap12() {
        RestAssured.given()
                .body("Joe")
                .post("/Soap12Rest/sync/soap12")
                .then()
                .statusCode(200)
                .body(Matchers.is("Hello Joe, Content-Type: application/soap+xml; action=\"helloAction\"; charset=UTF-8"));
    }

    @Test
    void contentType() {
        RestAssured.given()
                .body("Joe")
                .post("/Soap12Rest/sync/contentType")
                .then()
                .statusCode(200)
                .body(Matchers
                        .is("Hello Joe, Content-Type: application/soap+xml; action=\"helloAction\"; foo=bar; charset=UTF-8"));
    }

    @Test
    void contentTypeSoap12() {
        RestAssured.given()
                .body("Joe")
                .post("/Soap12Rest/sync/contentTypeSoap12")
                .then()
                .statusCode(200)
                .body(Matchers
                        .is("Hello Joe, Content-Type: application/soap+xml; action=\"helloAction\"; foo=bar; charset=UTF-8"));
    }

}
