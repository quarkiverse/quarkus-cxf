package io.quarkiverse.cxf.doc.it;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import org.assertj.core.api.Assertions;
import org.hamcrest.CoreMatchers;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import io.quarkiverse.antorassured.AntorAssured;
import io.quarkiverse.antorassured.LinkGroupFactory;
import io.quarkiverse.antorassured.LinkStream;
import io.quarkiverse.antorassured.ResourceResolver;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

@QuarkusTest
public class AntoraTest {

    private static final Logger log = Logger.getLogger(AntoraTest.class);

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

        final ZonedDateTime deadline = ZonedDateTime.parse("2025-11-27T23:59:59+01:00[Europe/Paris]");
        if (ZonedDateTime.now(ZoneId.of("Europe/Paris")).isBefore(deadline)) {
            ignorables.add("https://quarkus.io/blog/quarkus-3-30-released/");
        }

        LinkStream linkStream = AntorAssured
                .links()
                .excludeResolved(Pattern.compile("^\\Qhttp://localhost:808\\E[02].*"))
                .excludeEditThisPage()
                .overallTimeout(300_000L); // 5 min

        final String ghToken = System.getenv("GITHUB_TOKEN");
        if (ghToken == null) {
            if (Boolean.parseBoolean(System.getenv("GITHUB_ACTIONS"))) {
                Assertions.fail(
                        "Set GITHUB_TOKEN environment variable to test GitHub links - see https://docs.github.com/en/rest/repos/contents?apiVersion=2022-11-28");
            } else {
                log.warn(
                        "Set GITHUB_TOKEN environment variable to test GitHub links - see https://docs.github.com/en/rest/repos/contents?apiVersion=2022-11-28");
                linkStream = linkStream
                        .excludeResolved(Pattern.compile("https://github.com/[^/]+/[^/]+/(:?blob|tree)/.*"));
            }
        } else {
            log.info("GITHUB_TOKEN set");
            linkStream = linkStream
                    .group(LinkGroupFactory.gitHubRawBlobLinks(ghToken))
                    .endGroup();
        }

        linkStream
                .validate()
                .ignore(err -> ignorables.contains(err.uri().resolvedUri()))
                .assertValid();

    }
}
