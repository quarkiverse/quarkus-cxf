package io.quarkiverse.cxf.deployment.test.client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import jakarta.inject.Inject;
import jakarta.jws.WebService;
import jakarta.xml.ws.AsyncHandler;
import jakarta.xml.ws.Response;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.function.Executable;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkiverse.cxf.deployment.test.client.model.HelloResponse;
import io.quarkiverse.cxf.deployment.test.client.model.HelloService;
import io.quarkiverse.cxf.mutiny.CxfMutinyUtils;
import io.quarkus.logging.Log;
import io.quarkus.test.QuarkusUnitTest;
import io.vertx.core.Vertx;

public class WorkerDispatchTimeoutTest {
    private static final int MAX_THREADS = 4;

    private static final String DISPATCH_TIMEOUT = "100";
    private static final String DELAY = "1000";

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addPackage(HelloService.class.getPackage()))
            .overrideConfigKey("quarkus.cxf.client.worker-dispatch-timeout", DISPATCH_TIMEOUT)
            .overrideConfigKey("quarkus.thread-pool.max-threads", String.valueOf(MAX_THREADS))
            .overrideConfigKey("quarkus.log.category.\"io.quarkiverse.cxf.QuarkusJaxWsProxyFactoryBean\".level", "DEBUG")
            .overrideConfigKey("quarkus.log.category.\"io.quarkiverse.cxf.mutiny.CxfMutinyUtils\".level", "DEBUG")
            /*
             * Avoid too many warnings about uncaught exceptions due to requests being processed after shut down - that's
             * expected
             */
            .overrideConfigKey("quarkus.log.category.\"org.jboss.threads.errors\".level", "FATAL")
            .overrideConfigKey("quarkus.log.category.\"io.quarkus.vertx.core.runtime.VertxCoreRecorder\".level", "FATAL")
            .overrideConfigKey("quarkus.log.category.\"org.apache.cxf.phase.PhaseInterceptorChain\".level", "ERROR")

            .overrideConfigKey("quarkus.cxf.endpoint.\"/hello\".implementor", HelloServiceImpl.class.getName())
            .overrideConfigKey("quarkus.cxf.client.hello.client-endpoint-url", "http://localhost:8081/services/hello")
            .overrideConfigKey("quarkus.cxf.client.hello.service-interface", HelloService.class.getName());

    @CXFClient("hello")
    HelloService hello;

    @Inject
    Vertx vertx;

    @Test
    public void workerDispatchTimeout() throws Throwable {
        withWorkersBlocked(() -> {
            final CompletableFuture<Response<HelloResponse>> callback = new CompletableFuture<>();
            final Future<?> responseFuture = onEventLoop(() -> hello.helloAsync("World", callback::complete));

            final Response<HelloResponse> response = callback.get(10, TimeUnit.SECONDS);
            Assertions.assertThatThrownBy(response::get)
                    .rootCause().isInstanceOf(RejectedExecutionException.class)
                    .message().contains("Unable to dispatch SOAP client call within " + DISPATCH_TIMEOUT
                            + " ms on a worker thread due to worker thread pool exhaustion");
            Assertions.assertThatThrownBy(() -> responseFuture.get(10, TimeUnit.SECONDS))
                    .rootCause().isInstanceOf(RejectedExecutionException.class)
                    .message().contains("Unable to dispatch SOAP client call within " + DISPATCH_TIMEOUT
                            + " ms on a worker thread due to worker thread pool exhaustion");
        });
    }

    @Test
    public void workerDispatchTimeoutCxfMutinyUtils() throws Throwable {
        withWorkersBlocked(() -> {
            final CompletableFuture<String> response = mutinyHello();
            Assertions.assertThatThrownBy(() -> response.get(10, TimeUnit.SECONDS))
                    .rootCause().isInstanceOf(RejectedExecutionException.class)
                    .message().contains("Unable to dispatch SOAP client call within " + DISPATCH_TIMEOUT
                            + " ms on a worker thread due to worker thread pool exhaustion");
        });
    }

    @Test
    public void slowResponseDoesNotTriggerWorkerDispatchTimeout() throws Exception {
        final CompletableFuture<Response<HelloResponse>> callback = new CompletableFuture<>();
        final Future<?> responseFuture = onEventLoop(() -> hello.helloAsync("World", callback::complete));

        Assertions.assertThat(callback.get(10, TimeUnit.SECONDS).get().getReturn())
                .isEqualTo("Hello World from HelloServiceImpl");
        responseFuture.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void slowResponseDoesNotTriggerWorkerDispatchTimeoutCxfMutinyUtils() throws Exception {
        Assertions.assertThat(mutinyHello().get(10, TimeUnit.SECONDS))
                .isEqualTo("Hello World from HelloServiceImpl");
    }

    private CompletableFuture<String> mutinyHello() throws Exception {
        return onEventLoop(() -> CxfMutinyUtils
                .<HelloResponse> toUni(handler -> hello.helloAsync("World", handler))
                .map(HelloResponse::getReturn)
                .subscribeAsCompletionStage().toCompletableFuture());
    }

    private <T> T onEventLoop(Supplier<T> action) throws Exception {
        return CompletableFuture.supplyAsync(() -> {
            Assertions.assertThat(Thread.currentThread().getName()).startsWith("vert.x-eventloop-thread-");
            return action.get();
        }, command -> vertx.runOnContext(v -> command.run())).get(10, TimeUnit.SECONDS);
    }

    private void withWorkersBlocked(Executable test) throws Throwable {
        final CountDownLatch workersStarted = new CountDownLatch(MAX_THREADS);
        final CountDownLatch releaseWorkers = new CountDownLatch(1);
        final List<CompletableFuture<Void>> workers = new ArrayList<>();
        try {
            /*
             * Occupy all Vert.x worker threads before calling the client
             * by submitting a blocking task that waits for being unblocked by
             * releaseWorkers.countDown() done below,
             * after asserting the real SOAP client call's dispatch timeout.
             *
             * In the past, we were trying to simulate that condition by quickly sending a finite number of async SOAP requests.
             * That worked only sometimes, because those could fill the Vert.x queue, while the workers were still initializing.
             */
            for (int i = 0; i < MAX_THREADS; i++) {
                workers.add(vertx.<Void> executeBlocking(() -> {
                    workersStarted.countDown();
                    releaseWorkers.await();
                    return null;
                }, false).toCompletionStage().toCompletableFuture());
            }
            Assertions.assertThat(workersStarted.await(10, TimeUnit.SECONDS))
                    .as("all %d worker threads are occupied", MAX_THREADS).isTrue();
            Log.infof("All %d worker threads are occupied; testing the %s ms dispatch timeout", MAX_THREADS, DISPATCH_TIMEOUT);

            test.execute();
        } finally {
            releaseWorkers.countDown();
            for (CompletableFuture<Void> worker : workers) {
                worker.get(10, TimeUnit.SECONDS);
            }
        }
    }

    @WebService(serviceName = "HelloService")
    public static class HelloServiceImpl implements HelloService {

        @Override
        public Response<HelloResponse> helloAsync(String person) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Future<?> helloAsync(String arg0, AsyncHandler<HelloResponse> asyncHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String hello(String person) {
            if (person.equals("Exception")) {
                throw new RuntimeException("Expected exception");
            }
            try {
                long delay = Long.parseLong(DELAY);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            } catch (NumberFormatException e) {
            }
            return "Hello " + person + " from " + getClass().getSimpleName();
        }
    }

}
