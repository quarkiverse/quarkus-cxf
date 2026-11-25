package io.quarkiverse.cxf.it.versions;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class HttpVersionsTest {
    @ParameterizedTest
    @CsvSource({ "strict,HTTP/2.0", "upgrade,HTTP/2.0", "legacy,HTTP/2.0", "http1,HTTP/1.1" })
    void largeSoapRequest(String client, String protocol) {
        for (int i = 0; i < 2; i++) {
            RestAssured.given().contentType("text/plain").body("x".repeat(100_000))
                    .post("/http-versions/" + client)
                    .then().statusCode(200).body(is(protocol + ":100000"));
        }
    }
}
