package io.quarkiverse.cxf.metrics.deployment.test;

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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkiverse.cxf.metrics.deployment.test.client.model.HelloResponse;
import io.quarkiverse.cxf.metrics.deployment.test.client.model.HelloService;
import io.quarkiverse.cxf.mutiny.CxfMutinyUtils;
import io.quarkiverse.cxf.vertx.http.client.VertxHttpClientHTTPConduit.TimeoutIOException;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class ReceiveTimeoutLaggyServerTest {

    private static Logger log = Logger.getLogger(ReceiveTimeoutLaggyServerTest.class);

    private static final long DELAY = 1000L;
    private static final int CLIENT_POOL_SIZE = 5;
    private static final int ITERATION_COUNT = 2;

    @RegisterExtension
    public static final QuarkusUnitTest test = createTest();

    private static QuarkusUnitTest createTest() {
        final LaggyServer server = new LaggyServer(DELAY, CLIENT_POOL_SIZE * 2);
        final String baseUrl = "http://localhost:" + server.actualPort() + "/";

        return new QuarkusUnitTest()
                .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                        .addClass(LaggyServer.class)
                        .addPackage(HelloService.class.getPackage()))

                .overrideConfigKey("quarkus.micrometer.binder.vertx.enabled", "true")
                // mute the stacktraces
                .overrideConfigKey("quarkus.log.category.\"org.apache.cxf.phase.PhaseInterceptorChain\".level", "ERROR")

                .overrideConfigKey("quarkus.cxf.client.helloVertx.client-endpoint-url", baseUrl)
                .overrideConfigKey("quarkus.cxf.client.helloVertx.service-interface", HelloService.class.getName())
                .overrideConfigKey("quarkus.cxf.client.helloVertx.http-conduit-factory", "VertxHttpClientHTTPConduitFactory")
                .overrideConfigKey("quarkus.cxf.client.helloVertx.metrics.enabled", "true")
                .overrideConfigKey("quarkus.cxf.client.helloVertx.receive-timeout", "100")
                .overrideConfigKey("quarkus.cxf.client.helloVertx.vertx.connection-pool.http1-max-size",
                        String.valueOf(CLIENT_POOL_SIZE))

                .setAfterAllCustomizer(server::close);
    }

    @CXFClient("helloVertx")
    HelloService helloVertx;

    @Inject
    MeterRegistry registry;

    @Test
    public void async() {
        assertClient(ReceiveTimeoutLaggyServerTest::assertClientsAsync);
    }

    @Test
    public void sync() {
        assertClient(ReceiveTimeoutLaggyServerTest::assertClientsSync);
    }

    private void assertClient(BiFunction<HelloService, Long, Map<String, Long>> callClients) {
        /*
         * We sent 5*2 requests, all of which will take longer than the configured receive-timeout.
         * Once all clients timeout, the queue must be empty and the number of active connections must be 0
         */

        /* Issue all the clients calls */
        Map<String, Long> resultMap = callClients.apply(helloVertx, 120_000L);
        log.info("Async client results " + resultMap);

        /* All of them must have timed out */
        Assertions.assertThat(resultMap.getOrDefault("receive-timeout-headers", 0L))
                .isEqualTo(CLIENT_POOL_SIZE * ITERATION_COUNT);

        /* After a very short delay, the number of active connections must fall down to 0 */
        Awaitility.await("http.client.queue.size{clientName=helloVertx} == 0.0")
                .atMost(Duration.ofMillis(100))
                .pollDelay(Duration.ofMillis(10))
                .until(() -> registry.get("http.client.connections").tag("clientName", "helloVertx").longTaskTimer()
                        .activeTasks(), val -> val == 0);

        /* ... and the queue must be empty as well */
        Awaitility.await("http.client.connections{clientName=helloVertx}/activeTasks == 0")
                .atMost(Duration.ofMillis(100))
                .pollDelay(Duration.ofMillis(10))
                .until(() -> registry.get("http.client.queue.size").tag("clientName", "helloVertx").gauge().value(),
                        val -> val == 0.0);

        // registry.getMeters()
        // .forEach(meter -> System.out.println(
        // meter.getId() + " -> " + meter.measure().iterator().next().getValue()));

        /*
         * A request with no delay at the server side must succeed now
         * That proves indirectly, that the timeouted connections were removed from the queue
         */
        Assertions.assertThat(helloSync(helloVertx, "Speedy")).isEqualTo("success");
    }

    static Map<String, Long> assertClientsAsync(HelloService hello, long timeout) {
        return Multi.createFrom().range(0, CLIENT_POOL_SIZE * ITERATION_COUNT)
                .onItem().transformToUni(i -> helloAsync(hello, "Joe"))
                .merge()
                .collect().with(Collectors.groupingBy(
                        result -> result,
                        Collectors.counting()))
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem(Duration.ofMillis(timeout))
                .assertCompleted()
                .getItem();
    }

    static Map<String, Long> assertClientsSync(HelloService hello, long timeout) {
        ExecutorService executor = Executors.newFixedThreadPool(CLIENT_POOL_SIZE);
        try {
            List<CompletableFuture<List<String>>> futures = new ArrayList<>();
            for (int i = 0; i < CLIENT_POOL_SIZE; i++) {
                futures.add(CompletableFuture.supplyAsync(() -> {
                    List<String> result = new ArrayList<>();
                    for (int j = 0; j < ITERATION_COUNT; j++) {
                        result.add(helloSync(hello, "Joe"));
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
            if (root instanceof TimeoutIOException && root.getMessage().contains("ms to receive response headers")) {
                return "receive-timeout-headers";
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
                    if (root instanceof TimeoutIOException && root.getMessage().contains("ms to receive response headers")) {
                        return "receive-timeout-headers";
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

    static class LaggyServer {
        private static final String soapResponseStart = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><ns2:helloResponse xmlns:ns2=\"http://test.deployment.cxf.quarkiverse.io/\"><return>Hello ";
        private static final String soapResponseEnd = "</return></ns2:helloResponse></soap:Body></soap:Envelope>";
        private static final Pattern REQUEST_PATTERN = Pattern.compile("<arg0>([^<]+)</arg0>");
        private final Vertx vertx;
        private final int actualPort;

        public LaggyServer(long delay, int workerPoolSize) {
            vertx = Vertx.vertx(new VertxOptions().setWorkerPoolSize(workerPoolSize));

            Router router = Router.router(vertx);
            router.route().handler(BodyHandler.create());
            router.post("/")
                    .handler(ctx -> {
                        AtomicBoolean clientGone = new AtomicBoolean(false);
                        ctx.request().connection().closeHandler(v -> clientGone.set(true));
                        ctx.put("clientGone", clientGone);
                        ctx.next();
                    })
                    .blockingHandler(context -> {
                        Matcher m = REQUEST_PATTERN.matcher(context.body().asString());
                        String person = m.find() ? m.group(1) : null;
                        // log.info("Serving " + person);

                        AtomicBoolean clientGone = context.get("clientGone");
                        if (!"Speedy".equals(person)) {
                            long deadline = System.currentTimeMillis() + delay;
                            while (!clientGone.get() && System.currentTimeMillis() < deadline) {
                                try {
                                    Thread.sleep(10);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    context.response().setStatusCode(500).end();
                                    return;
                                }
                            }
                        }
                        HttpServerResponse resp = context.response()
                                .setChunked(true)
                                .putHeader("Content-Type", "text/xml; charset=utf-8")
                                .setStatusCode(200);
                        resp.write(soapResponseStart);
                        resp.end(person + soapResponseEnd);
                    });

            final HttpServer server = vertx.createHttpServer(new HttpServerOptions())
                    .requestHandler(router)
                    .listen(-2)
                    .toCompletionStage()
                    .toCompletableFuture()
                    .join();
            this.actualPort = server.actualPort();
        }

        public int actualPort() {
            return actualPort;
        }

        public void close() {
            vertx.close().toCompletionStage().toCompletableFuture().join();
        }
    }

}
