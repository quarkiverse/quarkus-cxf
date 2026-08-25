package io.quarkiverse.cxf.metrics.it;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.assertj.core.api.Assertions;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
@QuarkusTestResource(LaggyCalculatorServerTestResource.class)
public class ReceiveTimeoutLaggyServerTest {

    private static final Logger log = Logger.getLogger(ReceiveTimeoutLaggyServerTest.class);

    @Test
    public void async() {
        assertClient("call-clients-async");
    }

    @Test
    public void sync() {
        assertClient("call-clients-sync");
    }

    private void assertClient(String endpoint) {
        /* Make sure the pool has no active connections and that the queue is empty initially */
        assertNoActiveConnections(endpoint, 2000, true);

        /*
         * We send CLIENT_POOL_SIZE * ITERATION_COUNT requests, all of which will take longer
         * than the configured receive-timeout.
         * Once all clients timeout, the queue must be empty and the number of active connections must be 0
         */
        Map<String, Long> resultMap = callClients(endpoint, LaggyCalculatorServerTestResource.CLIENT_POOL_SIZE,
                LaggyCalculatorServerTestResource.ITERATION_COUNT, 42);
        log.info("Client results for " + endpoint + ": " + resultMap);

        /* All of them must have timed out */
        Assertions.assertThat(resultMap.getOrDefault("receive-timeout-headers", 0L))
                .isEqualTo(
                        LaggyCalculatorServerTestResource.CLIENT_POOL_SIZE * LaggyCalculatorServerTestResource.ITERATION_COUNT);

        /* After a short delay, both pool metrics must go to 0 */
        assertNoActiveConnections(endpoint, 500, false);

        /*
         * A request with no delay at the server side must succeed now.
         * That proves indirectly that the timed-out connections were removed from the queue.
         */
        Map<String, Long> m = callClients(endpoint, 1, 1, 0);
        Assertions.assertThat(m).isEqualTo(Map.of("success", 1L));
    }

    private void assertNoActiveConnections(String endpoint, long timeoutMs, boolean meterNotFoundAllowed) {
        String clientKey = endpoint.contains("async") ? "laggyCalculatorAsync" : "laggyCalculatorSync";
        long deadline = System.currentTimeMillis() + timeoutMs;
        PoolMetrics pm = null;
        while (System.currentTimeMillis() < deadline) {
            pm = PoolMetrics.of(RestAssured.given()
                    .get("/laggy-calculator/pool-metrics/" + clientKey + "/" + meterNotFoundAllowed)
                    .then().statusCode(200)
                    .extract().body().asString());
            if (pm.equals(PoolMetrics.ZERO)) {
                return;
            }
        }
        throw new AssertionError("Pool metrics did not fall to 0, 0 within " + timeoutMs + " ms: " + pm);
    }

    private Map<String, Long> callClients(String endpoint, int threadCount, int iterationCount, int a) {
        if (threadCount == 1) {
            return merge(new HashMap<>(), callClient(endpoint, iterationCount, a));
        }
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            List<CompletableFuture<String[]>> futures = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                futures.add(CompletableFuture.supplyAsync(() -> callClient(endpoint, iterationCount, a), executor));
            }
            try {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(120, TimeUnit.SECONDS);
                Map<String, Long> result = new HashMap<>();
                for (CompletableFuture<String[]> f : futures) {
                    try {
                        merge(result, f.get());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    } catch (ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                }
                return result;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            } catch (ExecutionException | TimeoutException e) {
                throw new RuntimeException(e);
            }
        } finally {
            executor.shutdown();
        }

    }

    static Map<String, Long> merge(Map<String, Long> result, String[] list) {
        for (String status : list) {
            result.merge(status, 1L, (old, new_) -> old + new_);
        }
        return result;
    }

    private String[] callClient(String endpoint, int iterationCount, int a) {
        return RestAssured.given()
                .header("Content-Type", "application/json")
                .get("/laggy-calculator/" + endpoint + "/" + iterationCount + "/" + a)
                .then()
                .statusCode(200)
                .extract().body().asString().split("\\|");
    }

    record PoolMetrics(int activeConnections, int queueSize) {
        public static final PoolMetrics ZERO = new PoolMetrics(0, 0);

        static PoolMetrics of(String raw) {
            String[] parts = raw.split(",");
            return new PoolMetrics(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }
    }
}
