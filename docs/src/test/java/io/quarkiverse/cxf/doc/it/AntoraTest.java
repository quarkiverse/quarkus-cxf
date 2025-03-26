package io.quarkiverse.cxf.doc.it;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import io.quarkiverse.antorassured.AntorAssured;
import io.quarkiverse.antorassured.ResourceResolver;
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
                .get(ResourceResolver.autodetect().getBaseUri().resolve("index.html"))
                .then()
                .statusCode(200)
                .body(CoreMatchers.containsString("<h1 class=\"page\">Quarkus CXF</h1>"));
    }

    @Test
    public void linksValid() {

        final Set<String> ignorables = new LinkedHashSet<>();

        final ZonedDateTime deadline = ZonedDateTime.parse("2025-03-27T23:59:59+01:00[Europe/Paris]");
        if (ZonedDateTime.now(ZoneId.of("Europe/Paris")).isBefore(deadline)) {
            ignorables.add("https://quarkus.io/blog/quarkus-3-20-0-released/");
            ignorables.add("https://quarkus.io/blog/quarkus-3-21-0-released/");
        }

        AntorAssured
                .links()
                .excludeResolved(Pattern.compile("^\\Qhttp://localhost:808\\E[02].*"))
                .excludeEditThisPage()
                .validate()
                .ignore(err -> ignorables.contains(err.uri().resolvedUri()))
                .assertValid();

    }
}
