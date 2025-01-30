package io.quarkiverse.cxf.it.fastinfoset;

import static org.hamcrest.Matchers.is;

import java.time.Duration;

import org.hamcrest.CoreMatchers;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import io.quarkiverse.cxf.test.QuarkusCxfClientTestUtil;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class FastInfosetTest {

    private static final Logger log = Logger.getLogger(FastInfosetTest.class);

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
    void fastInfosetTextXml() {
        log.info("FastInfosetTest.fastInfosetTextXml()");

        QuarkusCxfClientTestUtil.printThreadDumpAtTimeout(
                () -> {
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
                    return null;
                },
                Duration.ofSeconds(5),
                log::info);
    }

    @Test
    void fastInfosetNative() {
        log.info("FastInfosetTest.fastInfosetNative()");
        QuarkusCxfClientTestUtil.printThreadDumpAtTimeout(
                () -> {
                    RestAssured.given()
                            .body("FastInfoset")
                            .post("/fastinfoset/fastinfoset/hello")
                            .then()
                            .statusCode(200)
                            .body(is("Hello FastInfoset!"));
                    return null;
                },
                Duration.ofSeconds(5),
                log::info);
    }

}
