package io.quarkiverse.cxf.deployment.test.client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;
import jakarta.jws.WebService;
import jakarta.xml.ws.AsyncHandler;
import jakarta.xml.ws.Response;
import jakarta.xml.ws.soap.SOAPFaultException;

import org.apache.cxf.binding.soap.SoapFault;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkiverse.cxf.deployment.test.client.model.HelloResponse;
import io.quarkiverse.cxf.deployment.test.client.model.HelloService;
import io.quarkus.test.QuarkusUnitTest;
import io.vertx.core.Vertx;

public class AsyncHandlerTest {

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
    public void asyncHandlerCalledOnce() throws InterruptedException {

        final List<String> results = new ArrayList<>();
        hello.helloAsync("Joe", resp -> {
            synchronized (results) {
                try {
                    results.add(resp.get().getReturn());
                } catch (Exception e) {
                    results.add(e.getClass().getName() + ": " + e.getMessage());
                }
            }
        });

        Awaitility.waitAtMost(2, TimeUnit.SECONDS).until(() -> {
            synchronized (results) {
                return results.size() >= 1;
            }
        });
        /* Make sure the handler does not get called again */
        Thread.sleep(300);
        synchronized (results) {
            Assertions.assertThat(results).hasSize(1);
            Assertions.assertThat(results.get(0)).isEqualTo("Hello Joe from HelloServiceImpl");
        }
    }

    @Test
    public void exception() throws InterruptedException {

        AtomicReference<Exception> exception = new AtomicReference<>();

        Future<?> fut = hello.helloAsync("Exception", resp -> {
            try {
                resp.get();
                throw new IllegalStateException("!Unexpected exception!");
            } catch (Exception e) {
                exception.set(e);
            }
        });

        Awaitility.waitAtMost(2, TimeUnit.SECONDS).until(() -> exception.get() != null);
        Assertions.assertThat((Exception) exception.get().getCause())
                .isInstanceOf(SoapFault.class)
                .extracting(Exception::getMessage).isEqualTo("Expected exception");

        Assertions.assertThatThrownBy(fut::get).hasCauseExactlyInstanceOf(SOAPFaultException.class).cause()
                .hasMessage("Expected exception");
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
