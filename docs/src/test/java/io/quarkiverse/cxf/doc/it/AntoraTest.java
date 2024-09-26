package io.quarkiverse.cxf.doc.it;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import io.quarkiverse.antora.test.AntoraTestUtils;
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

    @Test
    public void externalLinks() {

        Set<String> ignorables = Set.of(
                /* known issue https://github.com/quarkiverse/antora-ui-quarkiverse/issues/86 */
                "http://quarkus.io/training",
                "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role",
                "http://schemas.xmlsoap.org/ws/2005/02/sc/dk/p_sha1",
                "http://www.w3.org/2009/xmlenc11#aes256-gcm");

        AntoraTestUtils.assertExternalLinksValid(err -> ignorables.contains(err.uri()));
    }
}
