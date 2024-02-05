package io.quarkiverse.cxf.it.fastinfoset;

import static org.hamcrest.Matchers.is;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class FastInfosetTest {
    @Test
    void gzip() {

        RestAssured.given()
                .header("Accept-Encoding", "gzip, identity;q=0, *;q=0")
                .contentType("text/xml")
                .body("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><ns2:hello xmlns:ns2=\"https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test\"><arg0>GZIP</arg0></ns2:hello></soap:Body></soap:Envelope>")
                .when()
                .post("/soap/gzip/hello")
                .then()
                .statusCode(200)
                .header("Content-Encoding", "gzip")
                .body(CoreMatchers.containsString("Hello GZIP"));

        RestAssured.given()
                .body("GZIP")
                .post("/fastinfoset/gzip/hello")
                .then()
                .statusCode(200)
                .body(is("Hello GZIP!"));

    }

    @Test
    void fastInfoset() {
        RestAssured.given()
                .accept("application/fastinfoset")
                .contentType("text/xml")
                .body("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><ns2:hello xmlns:ns2=\"https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test\"><arg0>FastInfoset</arg0></ns2:hello></soap:Body></soap:Envelope>")
                .when()
                .post("/soap/fastinfoset/hello")
                .then()
                .statusCode(200)
                .contentType("application/fastinfoset")
                .body(CoreMatchers.containsString("Hello FastInfoset"));

        RestAssured.given()
                .body("FastInfoset")
                .post("/fastinfoset/fastinfoset/hello")
                .then()
                .statusCode(200)
                .body(is("Hello FastInfoset!"));
    }

}
