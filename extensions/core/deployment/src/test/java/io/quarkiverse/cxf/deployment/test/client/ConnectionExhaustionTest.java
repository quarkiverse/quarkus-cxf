package io.quarkiverse.cxf.deployment.test.client;

import java.net.SocketTimeoutException;
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
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkiverse.cxf.deployment.test.client.model.HelloResponse;
import io.quarkiverse.cxf.deployment.test.client.model.HelloService;
import io.quarkiverse.cxf.mutiny.CxfMutinyUtils;
import io.quarkiverse.cxf.vertx.http.client.VertxHttpClientHTTPConduit.TimeoutIOException;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.mutiny.Uni;

public class ConnectionExhaustionTest {

  private static Logger log = Logger.getLogger(ConnectionExhaustionTest.class);

  private static final long DELAY = Runtime.version().feature() == 17 ? 200L : 100L;

  @RegisterExtension
  public static final QuarkusExtensionTest test = createTest();

  private static QuarkusExtensionTest createTest() {
    final SleepyServer server = new SleepyServer(DELAY);
    final String baseUrl = "http://localhost:" + server.actualPort() + "/";

    return new QuarkusExtensionTest()
        .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
            .addClass(SleepyServer.class)
            .addPackage(HelloService.class.getPackage()))

        // Vert.x conduit (has explicit connection pool configuration)
        .overrideConfigKey("quarkus.cxf.client.helloVertx.vertx.connection-pool.http1-max-size", "5")
        .overrideConfigKey("quarkus.cxf.client.helloVertx.client-endpoint-url", baseUrl)
        .overrideConfigKey("quarkus.cxf.client.helloVertx.service-interface", HelloService.class.getName())
        .overrideConfigKey("quarkus.cxf.client.helloVertx.http-conduit-factory", "VertxHttpClientHTTPConduitFactory")
        .overrideConfigKey("quarkus.cxf.client.helloVertx.receive-timeout", "100")
        // Time to wait for a free connection from the pool before failing
        .overrideConfigKey("quarkus.cxf.client.helloVertx.connection-request-timeout", "30000")

        // URLConnection conduit (JDK HttpURLConnection)
        // Uses JVM global http.maxConnections (default 5), not an explicit per-client pool.
        // Connections are reused via keep-alive, but not "pooled" like Vert.x.
        // When a connection times out, the socket is closed properly, so it's reclaimed.
        .overrideConfigKey("quarkus.cxf.client.helloUrlConnection.client-endpoint-url", baseUrl)
        .overrideConfigKey("quarkus.cxf.client.helloUrlConnection.service-interface", HelloService.class.getName())
        .overrideConfigKey("quarkus.cxf.client.helloUrlConnection.http-conduit-factory",
            "URLConnectionHTTPConduitFactory")
        .overrideConfigKey("quarkus.cxf.client.helloUrlConnection.receive-timeout", "100")
        // Time to wait for a free connection from the pool before failing
        .overrideConfigKey("quarkus.cxf.client.helloUrlConnection.connection-request-timeout", "30000")

        .setAfterAllCustomizer(server::close);
  }

  @CXFClient("helloVertx")
  HelloService helloVertx;
  @CXFClient("helloUrlConnection")
  HelloService helloUrlConnection;

  @Test
  public void receiveTimeoutVertxConduit() {
    // Test Vert.x conduit: should exhaust pool under timeout load
    Map<String, Long> resultMap = assertClientsSync(helloVertx, "Joe", 120_000, 20, 20);
    System.out.println("==== Vert.x received (timeouts) " + resultMap);
    long timeouts = resultMap.getOrDefault("receive-timeout", 0L);
    long success = resultMap.getOrDefault("success", 0L);

    // Pool exhaustion error: when all 5 connections are busy and no connection frees within
    // the connection-request-timeout (30000ms), a new request fails with this message:
    long poolExhaustedStorm = resultMap.entrySet()
        .stream()
        .filter(e -> e.getKey().contains("The timeout of 30000 ms has been exceeded when getting a connection"))
        .mapToLong(Map.Entry::getValue)
        .sum();

    System.out.println("==== Vert.x pool exhausted (storm) " + poolExhaustedStorm);
    System.out.println("==== Vert.x timeouts " + timeouts);
    System.out.println("==== Vert.x success " + success);

    Assertions.assertThat(timeouts + poolExhaustedStorm).isEqualTo(400L);
    Assertions.assertThat(poolExhaustedStorm).isGreaterThan(0L); // Vert.x exhausts pool
  }

  @Test
  public void receiveTimeoutUrlConnectionConduit() {
    // Test URLConnection conduit: should NOT exhaust pool (the workaround)
    Map<String, Long> resultMap = assertClientsSync(helloUrlConnection, "Joe", 120_000, 20, 20);
    System.out.println("==== URLConnection received (timeouts) " + resultMap);
    Assertions.assertThat(resultMap.get("receive-timeout")).isEqualTo(400L);

    // Pool exhaustion check: when all connections are busy and a request waits 30 seconds
    // for a free connection but none become available, CXF fails with:
    // "The timeout of 30000 ms has been exceeded when getting a connection"
    // URLConnection should NOT have this error because it closes dead connections properly.
    long poolExhaustedStorm = resultMap.entrySet()
        .stream()
        .filter(e -> e.getKey().contains("The timeout of 30000 ms has been exceeded when getting a connection"))
        .mapToLong(Map.Entry::getValue)
        .sum();

    Assertions.assertThat(poolExhaustedStorm).isEqualTo(0L); // URLConnection does NOT exhaust pool

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
      if (root instanceof SocketTimeoutException) {
        return "receive-timeout";
      }
      return root.getMessage();
    }
  }

  static Uni<String> helloAsync(HelloService hello, String person) {
    return CxfMutinyUtils.<HelloResponse>toUni(handler -> hello.helloAsync(person, handler))
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
