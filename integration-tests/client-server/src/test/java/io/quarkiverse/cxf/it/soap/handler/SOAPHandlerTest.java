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

        List<Future<String>> futures = new ArrayList<>(WORKERS_COUNT);
        try {

            for (int i = 0; i < WORKERS_COUNT; i++) {
                final Future<String> f = executor.submit(() -> {
                    final String result = RestAssured.given()
                            .get("/LargeSlowRest/" + endpoint)
                            .then()
                            .statusCode(200)
                            .extract().body().asString();
                    Log.infof("The service received %s headers", result);
                    return result;
                });
                futures.add(f);
            }

            // Ensure all tasks are completed
            for (Future<String> future : futures) {
                final String payload = future.get();
                Assertions.assertThat(payload).isEqualTo("1");
            }
        } finally {
            executor.shutdown();
        }
    }

}
