package io.quarkiverse.cxf.it.vertx.async;

import static org.hamcrest.CoreMatchers.is;

import org.assertj.core.api.Assumptions;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import io.quarkiverse.cxf.HTTPConduitImpl;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
class AsyncVertxClientTest {

    @Test
    void helloWithWsdl() {
        /* URLConnectionHTTPConduitFactory does not support async */
        Assumptions.assumeThat(HTTPConduitImpl.findDefaultHTTPConduitImpl())
                .isNotEqualTo(HTTPConduitImpl.URLConnectionHTTPConduitFactory);

        RestAssured.given()
                .body("Joe")
                .post("/RestAsyncWithWsdl/helloWithWsdl")
                .then()
                .statusCode(500)
                .body(CoreMatchers.containsString(
                        "You have attempted to perform a blocking operation on an IO thread."));

    }

    @Test
    void helloWithWsdlWithBlocking() {
        /* URLConnectionHTTPConduitFactory does not support async */
        Assumptions.assumeThat(HTTPConduitImpl.findDefaultHTTPConduitImpl())
                .isNotEqualTo(HTTPConduitImpl.URLConnectionHTTPConduitFactory);

        RestAssured.given()
                .body("Joe")
                .post("/RestAsyncWithWsdlWithBlocking/helloWithWsdlWithBlocking")
                .then()
                .statusCode(200)
                .body(is("Hello Joe from HelloWithWsdlWithBlocking"));
    }

    @Test
    void helloWithWsdlWithEagerInit() {
        /* URLConnectionHTTPConduitFactory does not support async */
        Assumptions.assumeThat(HTTPConduitImpl.findDefaultHTTPConduitImpl())
                .isNotEqualTo(HTTPConduitImpl.URLConnectionHTTPConduitFactory);

        RestAssured.given()
                .queryParam("person", "Max")
                .get("/RestAsyncWithWsdlWithEagerInit/helloWithWsdlWithEagerInit")
                .then()
                .statusCode(200)
                .body(is("Hello Max from HelloWithWsdlWithEagerInit"));
    }

    @Test
    void helloWithoutWsdl() {
        /* URLConnectionHTTPConduitFactory does not support async */
        Assumptions.assumeThat(HTTPConduitImpl.findDefaultHTTPConduitImpl())
                .isNotEqualTo(HTTPConduitImpl.URLConnectionHTTPConduitFactory);

        RestAssured.given()
                .queryParam("person", "Joe")
                .get("/RestAsyncWithoutWsdl/helloWithoutWsdl")
                .then()
                .statusCode(200)
                .body(is("Hello Joe from HelloWithoutWsdl"));
    }

    @Test
    void helloWithoutWsdlWithBlocking() {
        /* URLConnectionHTTPConduitFactory does not support async */
        Assumptions.assumeThat(HTTPConduitImpl.findDefaultHTTPConduitImpl())
                .isNotEqualTo(HTTPConduitImpl.URLConnectionHTTPConduitFactory);

        RestAssured.given()
                .queryParam("person", "Joe")
                .get("/RestAsyncWithoutWsdlWithBlocking/helloWithoutWsdlWithBlocking")
                .then()
                .statusCode(200)
                .body(is("Hello Joe from HelloWithoutWsdlWithBlocking"));
    }

}
