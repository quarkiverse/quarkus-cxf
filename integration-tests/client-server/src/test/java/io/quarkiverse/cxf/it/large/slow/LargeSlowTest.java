package io.quarkiverse.cxf.it.large.slow;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.assertj.core.api.Assertions;
import org.eclipse.microprofile.config.ConfigProvider;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import io.quarkus.logging.Log;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
class LargeSlowTest {
    private static final Logger log = Logger.getLogger(LargeSlowTest.class);
    private static final int KiB = 1024;
    private static final int DELAY_MS = 4000;
    private static final int PAYLOAD_SIZE = 9 * KiB;
    private static final int WORKERS_COUNT = 5;

    @Test
    void asyncLargeSlow() throws InterruptedException, ExecutionException {
        assertEndpoint("async/largeSlow");
    }

    @Test
    void syncLargeSlow() throws InterruptedException, ExecutionException {
        assertEndpoint("sync/largeSlow");
    }

    private void assertEndpoint(String endpoint) throws InterruptedException, ExecutionException {
        final ExecutorService executor = Executors.newFixedThreadPool(WORKERS_COUNT);
        final long startTime = System.currentTimeMillis();

        List<Future<String>> futures = new ArrayList<>(WORKERS_COUNT);
        try {

            for (int i = 0; i < WORKERS_COUNT; i++) {
                final Future<String> f = executor.submit(() -> {
                    Log.infof("Sending a request with delay %d ms", DELAY_MS);
                    String result = RestAssured.given()
                            .queryParam("sizeBytes", PAYLOAD_SIZE)
                            .queryParam("clientDeserializationDelayMs", DELAY_MS)
                            .queryParam("serviceExecutionDelayMs", 0)
                            .get("/LargeSlowRest/" + endpoint)
                            .then()
                            .statusCode(200)
                            .extract().body().asString();
                    Log.infof("Received payload of size %d", result.length());
                    return result;
                });
                futures.add(f);
            }

            // Ensure all tasks are completed
            for (Future<String> future : futures) {
                final String payload = future.get();
                Assertions.assertThat(payload).hasSize(PAYLOAD_SIZE);
            }
        } finally {
            executor.shutdown();
        }
        /*
         * Asserting that the requests pass in DELAY_MS plus some smallish constant time proves that their execution
         * does not block each other. Otherwise, it would take nearly WORKERS_COUNT times DELAY_MS.
         */
        long diff = System.currentTimeMillis() - startTime;
        Log.infof("%d slow (%d ms) requests passed in %d ms in parallel", WORKERS_COUNT, DELAY_MS, diff);
        Assertions.assertThat(diff).isLessThanOrEqualTo(DELAY_MS + 2000);

        /* Make sure the Thread.sleep() was not removed from LargeSlowOutput.setDelayMs(int) */
        Assertions.assertThat(diff).isGreaterThan(DELAY_MS);
    }

    @Test
    void asyncLargeSlowReceiveTimeout() throws InterruptedException, ExecutionException {
        log.info("Starting LargeSlowTest.asyncLargeSlowReceiveTimeout()");
        assertTimeout("async/largeSlowReceiveTimeout");
    }

    @Test
    void syncLargeSlowReceiveTimeout() throws InterruptedException, ExecutionException {
        log.info("Starting LargeSlowTest.syncLargeSlowReceiveTimeout()");
        assertTimeout("sync/largeSlowReceiveTimeout");
    }

    static void assertTimeout(String endpoint) {
        final int sizeBytes = 5;
        RestAssured.given()
                .queryParam("sizeBytes", sizeBytes)
                .queryParam("clientDeserializationDelayMs", 0)
                .queryParam("serviceExecutionDelayMs", 500)
                .get("/LargeSlowRest/" + endpoint)
                .then()
                .statusCode(500)
                .body(
                        CoreMatchers
                                .is(
                                        Matchers.oneOf(
                                                "Timeout waiting 100 ms to receive response headers from http://localhost:8081/soap/largeSlow",
                                                "Read timed out" // with export QUARKUS_CXF_DEFAULT_HTTP_CONDUIT_FACTORY=URLConnectionHTTPConduitFactory
                                        )));
    }

    /**
     * Make sure that our static copy is the same as the WSDL served by the container
     *
     * @throws IOException
     */
    @Test
    void wsdlUpToDate() throws IOException {
        final int port = ConfigProvider.getConfig()
                .getValue("quarkus.http.test-port", Integer.class);
        final String wsdlUrl = "http://localhost:" + port + "/soap/largeSlow?wsdl";
        Path staticCopyPath = Paths.get("src/main/resources/wsdl/LargeSlow.wsdl");

        final String expected = RestAssured.given()
                .get(wsdlUrl)
                .then()
                .statusCode(200)
                .extract().body().asString();

        if (!Files.isRegularFile(staticCopyPath)) {
            /*
             * This test can be run from the test jar on Quarkus Platform
             * In that case target/classes does not exist an we have to copy
             * what's needed manually
             */
            staticCopyPath = Paths.get("target/classes/wsdl/LargeSlow.wsdl");
            Files.createDirectories(staticCopyPath.getParent());
            try (InputStream in = LargeSlowTest.class.getClassLoader()
                    .getResourceAsStream("wsdl/LargeSlow.wsdl")) {
                Files.copy(in, staticCopyPath, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        /* The changing Docker IP address in the WSDL should not matter */
        final String sanitizerRegex = "<soap:address location=\"http://[^/]*/calculator-ws/CalculatorService\"></soap:address>";
        final String staticCopyContent = Files
                .readString(staticCopyPath, StandardCharsets.UTF_8)
                .replaceAll(sanitizerRegex, "");

        if (!expected.replaceAll(sanitizerRegex, "").equals(staticCopyContent)) {
            Files.writeString(staticCopyPath, expected, StandardCharsets.UTF_8);
            Assertions.fail("The static WSDL copy in " + staticCopyPath
                    + " went out of sync with the WSDL served by the container. The content was updated by the test, you just need to review and commit the changes.");
        }

    }

}
