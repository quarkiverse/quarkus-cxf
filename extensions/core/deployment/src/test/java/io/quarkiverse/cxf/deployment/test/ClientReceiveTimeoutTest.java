package io.quarkiverse.cxf.deployment.test;

import java.net.SocketTimeoutException;

import jakarta.inject.Inject;
import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import jakarta.xml.ws.WebServiceException;

import org.apache.cxf.frontend.ClientProxy;
import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.cxf.HTTPConduitImpl;
import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkiverse.cxf.vertx.http.client.VertxHttpClientHTTPConduit;
import io.quarkiverse.cxf.vertx.http.client.VertxHttpClientHTTPConduit.TimeoutIOException;
import io.quarkus.logging.Log;
import io.quarkus.test.QuarkusUnitTest;

public class ClientReceiveTimeoutTest {

    /* Receive timeout is much shorter than the Thread.sleep() in the endpoint impl. */
    private static final String RECEIVE_TIMEOUT = "100";

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(HelloService.class, SlowHelloServiceImpl.class))
            .overrideConfigKey("quarkus.cxf.endpoint.\"/hello\".implementor",
                    SlowHelloServiceImpl.class.getName())

            .overrideConfigKey("quarkus.cxf.client.helloUrlConnection.client-endpoint-url",
                    "http://localhost:8081/services/hello")
            .overrideConfigKey("quarkus.cxf.client.helloUrlConnection.service-interface", HelloService.class.getName())
            .overrideConfigKey("quarkus.cxf.client.helloUrlConnection.receive-timeout", RECEIVE_TIMEOUT)
            .overrideConfigKey("quarkus.cxf.client.helloUrlConnection.http-conduit-factory",
                    HTTPConduitImpl.URLConnectionHTTPConduitFactory.name())

            .overrideConfigKey("quarkus.cxf.client.helloVertxClient.client-endpoint-url",
                    "http://localhost:8081/services/hello")
            .overrideConfigKey("quarkus.cxf.client.helloVertxClient.service-interface", HelloService.class.getName())
            .overrideConfigKey("quarkus.cxf.client.helloVertxClient.receive-timeout", RECEIVE_TIMEOUT)
            .overrideConfigKey("quarkus.cxf.client.helloVertxClient.http-conduit-factory",
                    HTTPConduitImpl.VertxHttpClientHTTPConduitFactory.name());

    @Inject
    @CXFClient("helloUrlConnection")
    HelloService helloUrlConnection;

    @Inject
    @CXFClient("helloVertxClient")
    HelloService helloVertxClient;

    /**
     * Ensures that the client call fails when its ReceiveTimeout is shorter than the intentionally slow endpoint
     * response.
     */
    @Test
    public void helloUrlConnection() {

        Assertions
                .assertThatExceptionOfType(WebServiceException.class)
                .isThrownBy(() -> helloUrlConnection.hello("Joe"))
                .withRootCauseInstanceOf(SocketTimeoutException.class);
    }

    @Test
    public void helloVertxClient() {
        Assertions.assertThat(ClientProxy.getClient(helloVertxClient).getConduit())
                .isInstanceOf(VertxHttpClientHTTPConduit.class);

        Assertions
                .assertThatExceptionOfType(WebServiceException.class)
                .isThrownBy(() -> helloVertxClient.hello("Joe"))
                .havingRootCause()
                .isInstanceOf(TimeoutIOException.class)
                .withMessage("Timeout waiting " + RECEIVE_TIMEOUT
                        + " ms to receive response headers from http://localhost:8081/services/hello");

    }

    @WebService
    public interface HelloService {

        @WebMethod
        String hello(String person);

    }

    @WebService(endpointInterface = "io.quarkiverse.cxf.deployment.test.ClientReceiveTimeoutTest$HelloService", serviceName = "HelloService")
    public static class SlowHelloServiceImpl implements HelloService {

        @Override
        public String hello(String person) {
            int delay = 500;
            try {
                Log.infof("Sleeping %d ms", delay);
                Thread.sleep(delay);
                return "Hello " + person;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
    }

}
