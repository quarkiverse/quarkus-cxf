package io.quarkiverse.cxf.deployment.test.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;
import jakarta.jws.WebService;
import jakarta.xml.ws.AsyncHandler;
import jakarta.xml.ws.Response;

import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

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

            .overrideConfigKey("quarkus.cxf.endpoint.\"/hello\".implementor", HelloServiceImpl.class.getName())
            .overrideConfigKey("quarkus.cxf.client.hello.client-endpoint-url", "http://localhost:8081/services/hello")
            .overrideConfigKey("quarkus.cxf.client.hello.service-interface", HelloService.class.getName());

    @CXFClient("hello")
    HelloService hello;

    @Inject
    Vertx vertx;

    @Test
    public void workerDispatchTimeout() throws InterruptedException {

        final Set<String> threadNames = new HashSet<>();
        final List<Future<?>> futures = new ArrayList<>();

        /* We want to see some requests to fail due to the worker thread pool exhaustion */
        final AtomicReference<Exception> firstException = new AtomicReference<>();

        int cnt = 0;
        final int maxCnt = 200;
        /* Submit more and more requests till some of them timeouts due to worker thread pool exhaustion */
        while (cnt++ <= maxCnt && firstException.get() == null) {
            vertx.runOnContext(v -> {
                if (firstException.get() == null) {
                    Future<?> fut = hello.helloAsync(DELAY, resp -> {
                        firstException.updateAndGet(oldValue -> {
                            if (oldValue != null) {
                                return oldValue;
                            }
                            try {
                                resp.get().getReturn();
                                return null;
                            } catch (Exception e) {
                                return e;
                            }
                        });
                    });
                    synchronized (futures) {
                        threadNames.add(Thread.currentThread().getName());
                        futures.add(fut);
                    }
                }
            });
        }
        Awaitility.waitAtMost(10, TimeUnit.SECONDS).until(() -> firstException.get() != null);

        Assertions.assertThat(firstException.get())
                .rootCause().isInstanceOf(RejectedExecutionException.class)
                .message().contains("Unable to dispatch SOAP client call within " + DISPATCH_TIMEOUT
                        + " ms on a worker thread due to worker thread pool exhaustion");

        synchronized (futures) {
            /* Make sure we always called the client on the event loop */
            Assertions.assertThat(threadNames.iterator().next()).startsWith("vert.x-eventloop-thread-");
            Assertions.assertThat(threadNames).hasSize(1);

            /* Check the results received via futures */
            Log.infof("Got %d futures", futures.size());

            boolean futTimeoutExists = futures.stream()
                    .filter(Future::isDone) // do not wait for uncompleted futures.
                    // At least one we look for should be finished already
                    .map(f -> {
                        try {
                            return ((HelloResponse) f.get()).getReturn();
                        } catch (Exception e) {
                            return e.getClass().getName() + ": " + e.getMessage();
                        }
                    })
                    .filter(msg -> msg.contains(
                            "Unable to dispatch SOAP client call within 100 ms on a worker thread due to worker thread pool exhaustion"))
                    .findFirst().isPresent();
            Log.infof("futTimeoutExists %s", futTimeoutExists);
            Assertions.assertThat(futTimeoutExists).isTrue();
        }

    }

    @Test
    public void workerDispatchTimeoutCxfMutinyUtils() throws InterruptedException {

        /* We want to see some requests to fail due to the worker thread pool exhaustion */
        final AtomicReference<Throwable> firstException = new AtomicReference<>();

        int cnt = 0;
        final int maxCnt = 200;
        /* Submit more and more requests till some of them timeouts due to worker thread pool exhaustion */
        while (cnt++ <= maxCnt && firstException.get() == null) {
            vertx.runOnContext(v -> {
                if (firstException.get() == null) {
                    CxfMutinyUtils
                            .<HelloResponse> toUni(handler -> hello.helloAsync(DELAY, handler))
                            .map(HelloResponse::getReturn)
                            .subscribe()
                            .with(
                                    result -> {
                                    },
                                    e -> firstException.updateAndGet(oldValue -> oldValue != null ? oldValue : e));
                }
            });
        }
        Awaitility.waitAtMost(10, TimeUnit.SECONDS).until(() -> firstException.get() != null);

        Assertions.assertThat(firstException.get())
                .isInstanceOf(RejectedExecutionException.class)
                .message().contains("Unable to dispatch SOAP client call within " + DISPATCH_TIMEOUT
                        + " ms on a worker thread due to worker thread pool exhaustion");

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
                long delay = Long.parseLong(person);
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
