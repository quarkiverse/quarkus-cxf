package io.quarkiverse.cxf.deployment.test.client;

import java.time.Duration;
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
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.Assumptions;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.cxf.HTTPConduitImpl;
import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkiverse.cxf.deployment.test.client.model.HelloResponse;
import io.quarkiverse.cxf.deployment.test.client.model.HelloService;
import io.quarkiverse.cxf.mutiny.CxfMutinyUtils;
import io.quarkiverse.cxf.vertx.http.client.VertxHttpClientHTTPConduit.TimeoutIOException;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;

public class ReceiveTimeoutTest {

    private static Logger log = Logger.getLogger(ReceiveTimeoutTest.class);

    private static final long DELAY = Runtime.version().feature() == 17 ? 200L : 100L;
    private static final long RECEIVE_TIMEOUT = 3 * DELAY;
    private static final long TRANSPORT_AND_PROCESSING_DURATION = DELAY * 10;
    private static final int TASK_COUNT = 4;

    @RegisterExtension
    public static final QuarkusExtensionTest test = createTest();

    private static QuarkusExtensionTest createTest() {

        final SleepyServer server = new SleepyServer(DELAY);
        final String baseUrl = "http://localhost:" + server.actualPort() + "/";

        return new QuarkusExtensionTest()
                .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                        .addClass(SleepyServer.class)
                        .addPackage(HelloService.class.getPackage()))

                .overrideConfigKey("quarkus.cxf.client.hello1.client-endpoint-url", baseUrl)
                .overrideConfigKey("quarkus.cxf.client.hello1.service-interface", HelloService.class.getName())
                .overrideConfigKey("quarkus.cxf.client.hello1.vertx.connection-pool.http1-max-size", "1")
                .overrideConfigKey("quarkus.cxf.client.hello1.receive-timeout", String.valueOf(RECEIVE_TIMEOUT))
                .overrideConfigKey("quarkus.cxf.client.hello1.log.enabled", "pretty")

                .overrideConfigKey("quarkus.cxf.client.hello2.client-endpoint-url", baseUrl)
                .overrideConfigKey("quarkus.cxf.client.hello2.service-interface", HelloService.class.getName())
                .overrideConfigKey("quarkus.cxf.client.hello2.vertx.connection-pool.http1-max-size",
                        String.valueOf(TASK_COUNT))
                .overrideConfigKey("quarkus.cxf.client.hello2.receive-timeout", String.valueOf(RECEIVE_TIMEOUT))

                .overrideConfigKey("quarkus.cxf.client.hello3.client-endpoint-url", baseUrl)
                .overrideConfigKey("quarkus.cxf.client.hello3.service-interface", HelloService.class.getName())
                .overrideConfigKey("quarkus.cxf.client.hello3.vertx.connection-pool.http1-max-size", "1")
                .overrideConfigKey("quarkus.cxf.client.hello3.receive-timeout", "100")

                .overrideConfigKey("quarkus.cxf.client.hello4.client-endpoint-url", baseUrl)
                .overrideConfigKey("quarkus.cxf.client.hello4.service-interface", HelloService.class.getName())
                .overrideConfigKey("quarkus.cxf.client.hello4.vertx.connection-pool.http1-max-size", "5")
                .overrideConfigKey("quarkus.cxf.client.hello4.receive-timeout", "100")

                .setAfterAllCustomizer(server::close);
    }

    @CXFClient("hello1")
    HelloService hello1;
    @CXFClient("hello2")
    HelloService hello2;
    @CXFClient("hello3")
    HelloService hello3;
    @CXFClient("hello4")
    HelloService hello4;

    @Test
    public void receiveTimeout() {

        log.info("=== DELAY = " + DELAY);
        log.info("=== Runtime.version() = " + Runtime.version());
        log.info("=== Runtime.version().feature() = " + Runtime.version().feature());
        log.info("=== Runtime.version().interim() = " + Runtime.version().interim());
        log.info("=== Runtime.version().patch() = " + Runtime.version().patch());

        /* The receive timeout in URLConnectionHTTPConduitFactory works differently */
        Assumptions.assumeThat(HTTPConduitImpl.findDefaultHTTPConduitImpl())
                .isNotEqualTo(HTTPConduitImpl.URLConnectionHTTPConduitFactory);

        /*
         * Experiment 1:
         * * hello1 has http1-max-size 1, so it will not open any parallel connections
         * * The receive timeout of hello1 is cca 3 times longer than the delay of the server.
         * * We give it enough reserve because the server processing itself can take some time
         * * We expect all requests to succeed in TASK_COUNT * RECEIVE_TIMEOUT + some constant time
         * * Because both server and client go serially and because the start of receive timeout measurement
         * happens after the connection is ready, no receive timeout should occur
         */
        /*
         * RECEIVE_TIMEOUT is the time needed from client connect through send request,
         * waiting for DELAY ms to completely receiving the response.
         * RECEIVE_TIMEOUT has therefore be greater than DELAY
         */
        assert DELAY < RECEIVE_TIMEOUT;
        /*
         * We fire all client calls at once here in the test, but hello1 has http1-max-size 1, so it should not open
         * any parallel connections, but rather queue the tasks. That's what we want to confirm by this test.
         * If it opened parallel connections, then the fourth response could not completely be received
         * before its RECEIVE_TIMEOUT. That's because, first, the server can process only one request at time and
         * second, the processing of the requests #1, #2, #3 and #4 would take at least DELAY * TASK_COUNT
         * (In reality it would take even longer due to transport time, etc.)
         * DELAY * TASK_COUNT would be longer than RECEIVE_TIMEOUT.
         * Therefore we have to assert that DELAY * TASK_COUNT > RECEIVE_TIMEOUT
         */
        assert DELAY * TASK_COUNT > RECEIVE_TIMEOUT;
        long timeout1 = TASK_COUNT * RECEIVE_TIMEOUT + TRANSPORT_AND_PROCESSING_DURATION;
        {
            /* Async */
            long start1 = System.currentTimeMillis();
            Map<String, Long> results = assertClientsAsync(hello1, timeout1);
            Assertions.assertThat(results).isEqualTo(Map.of("success", (long) TASK_COUNT));
            /* Ensure that the requests are really processed serially */
            Assertions.assertThat(System.currentTimeMillis() - start1).isGreaterThanOrEqualTo(TASK_COUNT * DELAY);
        }
        {
            /* Sync */
            long start1 = System.currentTimeMillis();
            Map<String, Long> results = assertClientsSync(hello1, timeout1);
            Assertions.assertThat(results).isEqualTo(Map.of("success", (long) TASK_COUNT));
            /* Ensure that the requests are really processed serially */
            Assertions.assertThat(System.currentTimeMillis() - start1).isGreaterThanOrEqualTo(TASK_COUNT * DELAY);
        }

        /*
         * Experiment 2:
         * * hello2 has http1-max-size same as TASK_COUNT, so it will open as many parallel connections as the
         * number of requests
         * * The receive timeout of hello2 is cca 3 times longer than the delay of the server
         * * We give it enough reserve because the transport and server processing take some time
         * * We expect all requests to succeed or fail in RECEIVE_TIMEOUT + some constant time
         * * Because the server processes the requests serially, but the client connects all connections at once,
         * some of the requests must inevitably timeout.
         * * In theory, two requests might succeed if there was no overhead (because 2 * DELAY <= RECEIVE_TIMEOUT)
         * * In reality, typically only one will succeed and the rest will fail with receive timeout
         */
        long timeout2 = RECEIVE_TIMEOUT + TRANSPORT_AND_PROCESSING_DURATION;
        {
            /* Async */
            Map<String, Long> resultMap = assertClientsAsync(hello2, timeout2);
            Assertions.assertThat(resultMap.get("success")).isGreaterThan(0);
            Assertions.assertThat(resultMap.get("receive-timeout")).isGreaterThan(0);
            /* ... and ensure there were no other errors */
            Assertions.assertThat(resultMap.keySet()).containsExactlyInAnyOrder("success", "receive-timeout");
        }
        {
            /* Sync */
            Map<String, Long> resultMap = assertClientsSync(hello2, timeout2);
            Assertions.assertThat(resultMap.get("success")).isGreaterThan(0);
            Assertions.assertThat(resultMap.get("receive-timeout")).isGreaterThan(0);
            /* ... and ensure there were no other errors */
            Assertions.assertThat(resultMap.keySet()).containsExactlyInAnyOrder("success", "receive-timeout");
        }

        /*
         * Experiment 3
         * Make sure that a connection that got a receive timeout is usable right away
         */
        {
            /* Async */
            Assertions.assertThat(helloAsync(hello3, "Joe").await().indefinitely()).isEqualTo("receive-timeout");
            Assertions.assertThat(helloAsync(hello3, "Speedy").await().indefinitely()).isEqualTo("success");
        }
        {
            /* Sync */
            Assertions.assertThat(helloSync(hello3, "Joe")).isEqualTo("receive-timeout");
            Assertions.assertThat(helloSync(hello3, "Speedy")).isEqualTo("success");
        }

        /*
         * Experiment 4
         * Saturate the pool with receive timeouts only, hope to
         * observe https://github.com/quarkiverse/quarkus-cxf/issues/2240 at some point
         */
        {
            /* Sync */
            Map<String, Long> resultMap = assertClientsSync(hello4, "Joe", 60_000, 20, 20);
            System.out.println("==== received " + resultMap);
            Assertions.assertThat(resultMap.get("receive-timeout")).isEqualTo(400);
        }
        {
            Map<String, Long> resultMap = assertClientsSync(hello4, "Speedy", 60_000, 15, 5);
            System.out.println("==== received " + resultMap);
            Assertions.assertThat(resultMap.get("success")).isEqualTo(100);
        }
    }

    static Map<String, Long> assertClientsAsync(HelloService hello, long timeout) {
        return Multi.createFrom().range(0, TASK_COUNT)
                .onItem().transformToUni(i -> helloAsync(hello, "Joe"))
                .merge()
                .collect().with(Collectors.groupingBy(
                        result -> result,
                        Collectors.counting()))
                .invoke(count -> System.out.println("results: " + count))
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem(Duration.ofMillis(timeout))
                .assertCompleted()
                .getItem();
    }

    @SuppressWarnings("unchecked")
    static Map<String, Long> assertClientsSync(HelloService hello, long timeout) {
        return assertClientsSync(hello, "Joe", timeout, TASK_COUNT, 1);
    }

    static Map<String, Long> assertClientsSync(HelloService hello, String person, long timeout, int threadCount,
            int iterationCount) {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            List<CompletableFuture<List<String>>> futures = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                futures.add(CompletableFuture.supplyAsync(() -> {
                    List<String> result = new ArrayList<>();
                    for (int j = 0; j < iterationCount; j++) {
                        result.add(helloSync(hello, person));
                    }
                    return result;
                }, executor));
            }
            try {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(timeout, TimeUnit.MILLISECONDS);
                Map<String, Long> result = new HashMap<>();
                for (CompletableFuture<List<String>> f : futures) {
                    try {
                        for (String status : f.get()) {
                            result.merge(status, 1L, (old, new_) -> old + new_);
                        }
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

    private static String helloSync(HelloService hello, String person) {
        try {
            hello.hello(person);
            return "success";
        } catch (Exception e) {
            Throwable root = rootCause(e);
            if (root instanceof TimeoutIOException && root.getMessage().contains("receive response")) {
                return "receive-timeout";
            }
            return root.getMessage();
        }
    }

    static Uni<String> helloAsync(HelloService hello, String person) {
        return CxfMutinyUtils.<HelloResponse> toUni(handler -> hello.helloAsync(person, handler))
                .map(response -> "success")
                .onFailure()
                .recoverWithItem(t -> {
                    Throwable root = rootCause(t);
                    if (root instanceof TimeoutIOException && root.getMessage().contains("receive response")) {
                        return "receive-timeout";
                    }
                    return root.getMessage();
                });
    }

    static Throwable rootCause(Throwable t) {
        while (t.getCause() != null) {
            t = t.getCause();
        }
        return t;
    }

}
