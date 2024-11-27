package io.quarkiverse.cxf.doc.it;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashSet;
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

        Set<String> ignorables1 = Set.of(
                "http://quarkus.io/training",
                "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role",
                "http://schemas.xmlsoap.org/ws/2005/02/sc/dk/p_sha1",
                "http://www.w3.org/2009/xmlenc11#aes256-gcm");
        final Set<String> ignorables = new LinkedHashSet<>(ignorables1);

        final ZonedDateTime deadline = ZonedDateTime.parse("2024-11-28T23:59:59+01:00[Europe/Paris]");
        if (ZonedDateTime.now(ZoneId.of("Europe/Paris")).isBefore(deadline)) {
            ignorables.add("https://quarkus.io/blog/quarkus-3-17-0-released/");
        }

        AntoraTestUtils.assertExternalLinksValid(err -> ignorables.contains(err.uri()));
    }
}
