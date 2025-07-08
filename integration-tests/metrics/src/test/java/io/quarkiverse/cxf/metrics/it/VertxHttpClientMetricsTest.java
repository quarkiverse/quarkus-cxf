package io.quarkiverse.cxf.metrics.it;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;

@QuarkusTest
public class VertxHttpClientMetricsTest {
    private static final int WORKERS_COUNT = 128;
    private static final int ITERATIONS_COUNT = 4;

    @Test
    void vertxHttpClient() throws InterruptedException, ExecutionException {

        /* There should be no Vert.x HttpClient connection pool metrics available before we call anything */
        Assertions.assertThat(getMetrics().get("http.client.active.connections;clientName=vertxCalculator")).isNull();

        /*
         * Produce some concurrent load so that the client is forced to open as many connections as we allowed in
         * quarkus.cxf.client.vertxCalculator.vertx.connection-pool.http1-max-size
         */
        final ExecutorService executor = Executors.newFixedThreadPool(WORKERS_COUNT);
        final List<Future<Void>> futures = new ArrayList<>(WORKERS_COUNT);
        try {

            for (int i = 0; i < WORKERS_COUNT; i++) {
                final Future<Void> f = executor.submit(() -> {
                    for (int j = 0; j < ITERATIONS_COUNT; j++) {
                        RestAssured.given()
                                .queryParam("a", 7)
                                .queryParam("b", 4)
                                .get("/VertxHttpClientResource/addAsync")
                                .then()
                                .statusCode(200)
                                .body(Matchers.is("11"));

                    }
                    return null;
                });
                futures.add(f);
            }

            // Ensure all tasks are completed
            for (Future<Void> future : futures) {
                future.get();
            }
        } finally {
            executor.shutdown();
        }
        //System.out.println(getMetrics());
        /*
         * Finally make sure the metric shows the value we have set in
         * quarkus.cxf.client.vertxCalculator.vertx.connection-pool.http1-max-size
         */
        Assertions.assertThat(getMetrics().get("http.client.active.connections;clientName=vertxCalculator"))
                .isEqualTo(9.0f);
    }

    public static Map<String, Object> getMetrics() {
        final String body = RestAssured.given()
                .header("Content-Type", "application/json")
                .get("/q/metrics/json")
                .then()
                .statusCode(200)
                .extract().body().asString();
        final JsonPath jp = new JsonPath(body);
        return jp.getJsonObject("$");
    }

}
