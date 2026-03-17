package io.quarkiverse.saaj.it;

import static io.restassured.RestAssured.given;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkiverse.cxf.test.internal.QuarkusCxfInternalTestUtil;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class SaajTest {

    @Test
    public void ping() {
        given()
                .when()
                .post("/saaj/hello/Joe")
                .then()
                .statusCode(200)
                .body(
                        Matchers.hasXPath(
                                QuarkusCxfInternalTestUtil.anyNs("Envelope", "Body", "hello", "person")
                                        + "/text()",
                                CoreMatchers.is("Joe")));
    }

}
