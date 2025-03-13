package io.quarkiverse.cxf.it.soap.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.assertj.core.api.Assertions;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import io.quarkus.logging.Log;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
class SOAPHandlerTest {
    private static final Logger log = Logger.getLogger(SOAPHandlerTest.class);
    private static final int WORKERS_COUNT = 128;
    private static final int ITERATIONS_COUNT = 16;

    @Test
    void asyncLargeSlowSOAPHandler() throws InterruptedException, ExecutionException {
        assertEndpoint("async/largeSlowSOAPHandler");
    }

    @Test
    void syncLargeSlowSOAPHandler() throws InterruptedException, ExecutionException {
        assertEndpoint("sync/largeSlowSOAPHandler");
    }

    private void assertEndpoint(String endpoint) throws InterruptedException, ExecutionException {

        final ExecutorService executor = Executors.newFixedThreadPool(WORKERS_COUNT);

        List<Future<List<String>>> futures = new ArrayList<>(WORKERS_COUNT);
        try {

            for (int i = 0; i < WORKERS_COUNT; i++) {
                final int worker = i;
                final Future<List<String>> f = executor.submit(() -> {
                    final List<String> list = new ArrayList<>();
                    for (int j = 0; j < ITERATIONS_COUNT; j++) {
                        final String result = RestAssured.given()
                                .queryParam("worker", worker)
                                .queryParam("iteration", j)
                                .get("/LargeSlowRest/" + endpoint)
                                .then()
                                .statusCode(200)
                                .extract().body().asString();
                        Log.infof("The service received %s headers on iteration %d", result, j);
                        list.add(result);
                    }
                    return list;
                });
                futures.add(f);
            }

            final List<String> expected = new ArrayList<>();
            for (int j = 0; j < ITERATIONS_COUNT; j++) {
                expected.add("1");
            }
            // Ensure all tasks are completed
            for (Future<List<String>> future : futures) {
                final List<String> payload = future.get();
                Assertions.assertThat(payload).isEqualTo(expected);
            }
        } finally {
            executor.shutdown();
        }
    }

}
