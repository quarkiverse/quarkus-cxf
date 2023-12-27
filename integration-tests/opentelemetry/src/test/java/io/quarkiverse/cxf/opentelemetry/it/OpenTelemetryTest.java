package io.quarkiverse.cxf.opentelemetry.it;

import static io.restassured.RestAssured.given;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.trace.SpanKind;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;

@QuarkusTest
public class OpenTelemetryTest {

    @AfterEach
    void reset() {
        RestAssured
                .get("/opentelemetry/reset")
                .then()
                .statusCode(200);
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> getSpans().size() == 0);
    }

    @Test
    void span() {

        given()
                .body("Charles")
                .post("/opentelemetry/client/hello")
                .then()
                .statusCode(200)
                .body(CoreMatchers.equalTo("Hello Charles!"));

        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> getSpans().size() == 4);

        final List<Map<String, Object>> spans = getSpans();

        int i = 0;
        {
            /* Quarkus CXF service span */
            final Map<String, Object> span = spans.get(i++);
            Assertions.assertThat(span.get("kind")).isEqualTo(SpanKind.SERVER.toString());
            Assertions.assertThat(span.get("name")).isEqualTo("POST /soap/hello");

            final Map<?, ?> attribs = (Map<?, ?>) span.get("attributes");
            Assertions.assertThat(attribs.get("http.status_code")).isEqualTo(200);
            Assertions.assertThat(attribs.get("http.method")).isEqualTo("POST");
        }

        {
            /* quarkus-vertx-web span serving the Quarkus CXF service */
            final Map<String, Object> span = spans.get(i++);
            Assertions.assertThat(span.get("kind")).isEqualTo(SpanKind.SERVER.toString());
            Assertions.assertThat(span.get("name")).isEqualTo("POST /soap/");

            final Map<?, ?> attribs = (Map<?, ?>) span.get("attributes");
            Assertions.assertThat(attribs.get("http.status_code")).isEqualTo(200);
            Assertions.assertThat(attribs.get("http.method")).isEqualTo("POST");
        }

        {
            /* Quarkus CXF client span */
            final Map<String, Object> span = spans.get(i++);
            Assertions.assertThat(span.get("kind")).isEqualTo(SpanKind.CLIENT.toString());
            Assertions.assertThat(span.get("name")).isEqualTo("POST http://localhost:8081/soap/hello");

            final Map<?, ?> attribs = (Map<?, ?>) span.get("attributes");
            Assertions.assertThat(attribs.get("http.status_code")).isEqualTo(200);
            Assertions.assertThat(attribs.get("http.method")).isEqualTo("POST");
        }

        {
            /* quarkus-vertx-web span invoking the CXF client */
            final Map<String, Object> span = spans.get(i++);
            Assertions.assertThat(span.get("kind")).isEqualTo(SpanKind.SERVER.toString());
            Assertions.assertThat(span.get("name")).isEqualTo("POST /opentelemetry/client/hello");

            final Map<?, ?> attribs = (Map<?, ?>) span.get("attributes");
            Assertions.assertThat(attribs.get("http.status_code")).isEqualTo(200);
            Assertions.assertThat(attribs.get("http.method")).isEqualTo("POST");
        }

    }

    private static List<Map<String, Object>> getSpans() {
        return RestAssured.get("/opentelemetry/export").body().as(new TypeRef<>() {
        });
    }
}
