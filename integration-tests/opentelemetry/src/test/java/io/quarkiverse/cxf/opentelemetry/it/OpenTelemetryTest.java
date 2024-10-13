package io.quarkiverse.cxf.opentelemetry.it;

import static io.restassured.RestAssured.given;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.trace.SpanKind;
import io.quarkiverse.cxf.HTTPConduitImpl;
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
        final int spanCount;
        final HTTPConduitImpl defaultImpl = HTTPConduitImpl.findDefaultHTTPConduitImpl();
        switch (defaultImpl) {
            case VertxHttpClientHTTPConduitFactory:
                spanCount = 5;
                break;
            case URLConnectionHTTPConduitFactory:
                spanCount = 4;
                break;
            default:
                throw new IllegalArgumentException("Unexpected value: " + defaultImpl);
        }

        given()
                .body("Charles")
                .post("/opentelemetry/client/hello")
                .then()
                .statusCode(200)
                .body(CoreMatchers.equalTo("Hello Charles!"));

        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> getSpans().size() == spanCount);

        final List<Map<String, Object>> spans = getSpans();

        /* The ordering of the spans is not fully deterministic, so let's just check that all 4 are there */
        final Map<String, Map<String, Object>> spansByName = new LinkedHashMap<>();
        spans.forEach(span -> spansByName.put(span.get("name").toString(), span));
        Assertions.assertThat(spansByName.size()).isEqualTo(spanCount);

        {
            /* Quarkus CXF service span */
            final Map<String, Object> span = spansByName.get("POST /soap/hello");
            Assertions.assertThat(span.get("kind")).isEqualTo(SpanKind.SERVER.toString());
            Assertions.assertThat(span.get("name")).isEqualTo("POST /soap/hello");

            final Map<?, ?> attribs = (Map<?, ?>) span.get("attributes");
            Assertions.assertThat(attribs.get("http.response.status_code")).isEqualTo(200);
            Assertions.assertThat(attribs.get("http.request.method")).isEqualTo("POST");
        }

        {
            /* quarkus-vertx-web span serving the Quarkus CXF service */
            final Map<String, Object> span = spansByName.get("POST /soap/");
            Assertions.assertThat(span.get("kind")).isEqualTo(SpanKind.SERVER.toString());
            Assertions.assertThat(span.get("name")).isEqualTo("POST /soap/");

            final Map<?, ?> attribs = (Map<?, ?>) span.get("attributes");
            Assertions.assertThat(attribs.get("http.response.status_code")).isEqualTo(200);
            Assertions.assertThat(attribs.get("http.request.method")).isEqualTo("POST");
        }

        {
            /* Quarkus CXF client span */
            final Map<String, Object> span = spansByName.get("POST http://localhost:8081/soap/hello");
            Assertions.assertThat(span.get("kind")).isEqualTo(SpanKind.CLIENT.toString());
            Assertions.assertThat(span.get("name")).isEqualTo("POST http://localhost:8081/soap/hello");

            final Map<?, ?> attribs = (Map<?, ?>) span.get("attributes");
            Assertions.assertThat(attribs.get("http.response.status_code")).isEqualTo(200);
            Assertions.assertThat(attribs.get("http.request.method")).isEqualTo("POST");
        }

        if (spanCount == 5) {
            /* Vert.x client span */
            final Map<String, Object> span = spansByName.get("POST");
            Assertions.assertThat(span.get("kind")).isEqualTo(SpanKind.CLIENT.toString());
            Assertions.assertThat(span.get("name")).isEqualTo("POST");

            final Map<?, ?> attribs = (Map<?, ?>) span.get("attributes");
            Assertions.assertThat(attribs.get("http.response.status_code")).isEqualTo(200);
            Assertions.assertThat(attribs.get("http.request.method")).isEqualTo("POST");
        }

        {
            /* quarkus-vertx-web span invoking the CXF client */
            final Map<String, Object> span = spansByName.get("POST /opentelemetry/client/hello");
            Assertions.assertThat(span.get("kind")).isEqualTo(SpanKind.SERVER.toString());
            Assertions.assertThat(span.get("name")).isEqualTo("POST /opentelemetry/client/hello");

            final Map<?, ?> attribs = (Map<?, ?>) span.get("attributes");
            Assertions.assertThat(attribs.get("http.response.status_code")).isEqualTo(200);
            Assertions.assertThat(attribs.get("http.request.method")).isEqualTo("POST");
        }

    }

    @Test
    void traced() {
        final int spanCount;
        final HTTPConduitImpl defaultImpl = HTTPConduitImpl.findDefaultHTTPConduitImpl();
        switch (defaultImpl) {
            case VertxHttpClientHTTPConduitFactory:
                spanCount = 6;
                break;
            case URLConnectionHTTPConduitFactory:
                spanCount = 5;
                break;
            default:
                throw new IllegalArgumentException("Unexpected value: " + defaultImpl);
        }

        given()
                .body("Joe")
                .post("/opentelemetry/client/helloTraced")
                .then()
                .statusCode(200)
                .body(CoreMatchers.equalTo("Hello traced Joe"));

        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
            List<Map<String, Object>> spans = getSpans();
            //System.out.println("=== spans " + spans.size() + " " + spans);
            return spans.size() == spanCount;
        });

        final List<Map<String, Object>> spans = getSpans();

        /* The ordering of the spans is not fully deterministic, so let's just check that all 4 are there */
        final Map<String, Map<String, Object>> spansByName = new LinkedHashMap<>();
        spans.forEach(span -> spansByName.put(span.get("name").toString(), span));
        Assertions.assertThat(spansByName.size()).isEqualTo(spanCount);

        {
            /* Quarkus CXF service span */
            final Map<String, Object> span = spansByName.get("POST /soap/hello");
            Assertions.assertThat(span.get("kind")).isEqualTo(SpanKind.SERVER.toString());
            Assertions.assertThat(span.get("name")).isEqualTo("POST /soap/hello");

            final Map<?, ?> attribs = (Map<?, ?>) span.get("attributes");
            Assertions.assertThat(attribs.get("http.response.status_code")).isEqualTo(200);
            Assertions.assertThat(attribs.get("http.request.method")).isEqualTo("POST");

            /* TracedBean.helloTraced */
            final Map<String, Object> tracedSpan = spansByName.get("TracedBean.helloTracedSpan");
            Assertions.assertThat(tracedSpan.get("kind")).isEqualTo(SpanKind.INTERNAL.toString());
            Assertions.assertThat(tracedSpan.get("name")).isEqualTo("TracedBean.helloTracedSpan");

            /* TracedBean.helloTraced has the service as its parent */
            Assertions.assertThat(tracedSpan.get("parentSpanId")).isEqualTo(span.get("spanId"));
        }

        {
            /* quarkus-vertx-web span serving the Quarkus CXF service */
            final Map<String, Object> span = spansByName.get("POST /soap/");
            Assertions.assertThat(span.get("kind")).isEqualTo(SpanKind.SERVER.toString());
            Assertions.assertThat(span.get("name")).isEqualTo("POST /soap/");

            final Map<?, ?> attribs = (Map<?, ?>) span.get("attributes");
            Assertions.assertThat(attribs.get("http.response.status_code")).isEqualTo(200);
            Assertions.assertThat(attribs.get("http.request.method")).isEqualTo("POST");
        }

        {
            /* Quarkus CXF client span */
            final Map<String, Object> span = spansByName.get("POST http://localhost:8081/soap/hello");
            Assertions.assertThat(span.get("kind")).isEqualTo(SpanKind.CLIENT.toString());
            Assertions.assertThat(span.get("name")).isEqualTo("POST http://localhost:8081/soap/hello");

            final Map<?, ?> attribs = (Map<?, ?>) span.get("attributes");
            Assertions.assertThat(attribs.get("http.response.status_code")).isEqualTo(200);
            Assertions.assertThat(attribs.get("http.request.method")).isEqualTo("POST");
        }

        if (spanCount == 6) {
            /* Vert.x client span */
            final Map<String, Object> span = spansByName.get("POST");
            Assertions.assertThat(span.get("kind")).isEqualTo(SpanKind.CLIENT.toString());
            Assertions.assertThat(span.get("name")).isEqualTo("POST");

            final Map<?, ?> attribs = (Map<?, ?>) span.get("attributes");
            Assertions.assertThat(attribs.get("http.response.status_code")).isEqualTo(200);
            Assertions.assertThat(attribs.get("http.request.method")).isEqualTo("POST");
        }

        {
            /* quarkus-vertx-web span invoking the CXF client */
            final Map<String, Object> span = spansByName.get("POST /opentelemetry/client/helloTraced");
            Assertions.assertThat(span.get("kind")).isEqualTo(SpanKind.SERVER.toString());
            Assertions.assertThat(span.get("name")).isEqualTo("POST /opentelemetry/client/helloTraced");

            final Map<?, ?> attribs = (Map<?, ?>) span.get("attributes");
            Assertions.assertThat(attribs.get("http.response.status_code")).isEqualTo(200);
            Assertions.assertThat(attribs.get("http.request.method")).isEqualTo("POST");
        }

    }

    private static List<Map<String, Object>> getSpans() {
        return RestAssured.get("/opentelemetry/export").body().as(new TypeRef<>() {
        });
    }
}
