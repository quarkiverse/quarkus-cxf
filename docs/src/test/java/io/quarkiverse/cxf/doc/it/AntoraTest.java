package io.quarkiverse.cxf.doc.it;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

@QuarkusTest
public class AntoraTest {

    @Test
    public void antoraSite() throws TimeoutException, IOException, InterruptedException {
        RestAssured
                .given()
                .contentType(ContentType.HTML)
                .get("/quarkus-cxf/dev/index.html")
                .then()
                .statusCode(200)
                .body(CoreMatchers.containsString("<h1 class=\"page\">Quarkus CXF</h1>"));
    }

}
