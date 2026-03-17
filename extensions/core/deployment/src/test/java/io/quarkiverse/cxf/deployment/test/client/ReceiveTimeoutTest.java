package io.quarkiverse.cxf.deployment.test.client;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.Assumptions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import io.quarkiverse.cxf.HTTPConduitImpl;
import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkiverse.cxf.deployment.test.client.model.HelloResponse;
import io.quarkiverse.cxf.deployment.test.client.model.HelloService;
import io.quarkiverse.cxf.mutiny.CxfMutinyUtils;
import io.quarkiverse.cxf.vertx.http.client.VertxHttpClientHTTPConduit.TimeoutIOException;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;

public class ReceiveTimeoutTest {

    private static final int PORT = 8083;
    private static final long RECEIVE_TIMEOUT = 160L;
    private static final int TASK_COUNT = 4;

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addPackage(HelloService.class.getPackage()))

            .overrideConfigKey("quarkus.cxf.client.hello1.client-endpoint-url", "http://localhost:" + PORT + "/")
            .overrideConfigKey("quarkus.cxf.client.hello1.service-interface", HelloService.class.getName())
            .overrideConfigKey("quarkus.cxf.client.hello1.vertx.connection-pool.http1-max-size", "1")
            .overrideConfigKey("quarkus.cxf.client.hello1.receive-timeout", String.valueOf(RECEIVE_TIMEOUT))

            .overrideConfigKey("quarkus.cxf.client.hello2.client-endpoint-url", "http://localhost:" + PORT + "/")
            .overrideConfigKey("quarkus.cxf.client.hello2.service-interface", HelloService.class.getName())
            .overrideConfigKey("quarkus.cxf.client.hello2.vertx.connection-pool.http1-max-size", String.valueOf(TASK_COUNT))
            .overrideConfigKey("quarkus.cxf.client.hello2.receive-timeout", String.valueOf(RECEIVE_TIMEOUT));

    @CXFClient("hello1")
    HelloService hello1;
    @CXFClient("hello2")
    HelloService hello2;

    @Test
    public void receiveTimeout() throws InterruptedException, IOException {
        /* The receive timeout in URLConnectionHTTPConduitFactory works differently */
        Assumptions.assumeThat(HTTPConduitImpl.findDefaultHTTPConduitImpl())
                .isNotEqualTo(HTTPConduitImpl.URLConnectionHTTPConduitFactory);

        final long DELAY = 80;
        final HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        /* The server is able to process only one request at time */
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        server.setExecutor(executor);
        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                try {
                    String response = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><ns2:helloResponse xmlns:ns2=\"http://test.deployment.cxf.quarkiverse.io/\"><return>Hello Joe!</return></ns2:helloResponse></soap:Body></soap:Envelope>";
                    byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "text/xml; charset=utf-8");
                    exchange.sendResponseHeaders(200, bytes.length);
                    Thread.sleep(DELAY);

                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    exchange.sendResponseHeaders(500, 0);
                } finally {
                    exchange.close();
                }
            }
        });
        server.start();
        try {

            /*
             * Experiment 1:
             * * hello1 has http1-max-size 1, so it will not open any parallel connections
             * * The receive timeout of hello1 is cca twice as long as the delay of the server
             * * We give it enough reserve because com.sun.net.httpserver.HttpServer is not especially fast
             * * We expect all requests to succeed in TASK_COUNT * RECEIVE_TIMEOUT + some constant time
             * * Because both server and client go serially and because the start of receive timeout measurement
             * happens after the connection is ready, no receive timeout should occur
             */
            assert RECEIVE_TIMEOUT > DELAY;
            {
                /* Async */
                long start1 = System.currentTimeMillis();
                Map<String, Long> results = assertClients(hello1, TASK_COUNT * RECEIVE_TIMEOUT + 1000);
                Assertions.assertThat(results).isEqualTo(Map.of("success", (long) TASK_COUNT));
                long duration1 = System.currentTimeMillis() - start1;
                /* Ensure that the requests are really processed serially */
                Assertions.assertThat(duration1).isGreaterThanOrEqualTo(TASK_COUNT * DELAY);
            }

            /*
             * Experiment 2:
             * * hello2 has http1-max-size same as TASK_COUNT, so it will open as many parallel connections as the
             * number of requests
             * * The receive timeout of hello2 is cca twice as long as the delay of the server
             * * We give it enough reserve because com.sun.net.httpserver.HttpServer is not especially fast
             * * We expect all requests to succeed or fail in RECEIVE_TIMEOUT + some constant time
             * * Because the server processes the requests serially, but the client connects all connections at once,
             * some of the requests must inevitably timeout.
             * * In theory, two requests might succeed if there was no overhead (because 2 * DELAY <= RECEIVE_TIMEOUT)
             * * In reality, typically only one will succeed and the rest will fail with receive timeout
             */
            {
                /* Async */
                Map<String, Long> resultMap = assertClients(hello2, RECEIVE_TIMEOUT + 1000);
                Assertions.assertThat(resultMap.get("success")).isGreaterThan(0);
                Assertions.assertThat(resultMap.get("receive-timeout")).isGreaterThan(0);
                /* ... and ensure there were no other errors */
                Assertions.assertThat(resultMap.keySet()).containsExactlyInAnyOrder("success", "receive-timeout");
            }
        } finally {
            server.stop(0);
            executor.shutdown();
        }
    }

    static Map<String, Long> assertClients(HelloService hello, long timeout) {
        return Multi.createFrom().range(0, TASK_COUNT)
                .onItem().transformToUni(i -> hello(hello))
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

    static Uni<String> hello(HelloService hello) {
        return CxfMutinyUtils.<HelloResponse> toUni(handler -> hello.helloAsync("Joe", handler))
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
