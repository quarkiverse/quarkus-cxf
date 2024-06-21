package io.quarkiverse.cxf.deployment.test;

import jakarta.inject.Inject;
import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.transport.http.HTTPConduitFactory;
import org.apache.cxf.transport.http.HttpClientHTTPConduit;
import org.apache.cxf.transport.http.URLConnectionHTTPConduit;
import org.assertj.core.api.Assertions;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.cxf.CxfClientConfig.HTTPConduitImpl;
import io.quarkiverse.cxf.URLConnectionHTTPConduitFactory;
import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkiverse.cxf.deployment.test.GlobalConduitFactoryTest.HelloService;
import io.quarkiverse.cxf.vertx.http.client.VertxHttpClientHTTPConduit;
import io.quarkiverse.cxf.vertx.http.client.VertxHttpClientHTTPConduitFactory;
import io.quarkus.test.QuarkusUnitTest;

public class ClientConduitFactoryTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(HelloService.class, HelloServiceImpl.class))
            .overrideConfigKey("quarkus.cxf.endpoint.\"/hello\".implementor",
                    HelloServiceImpl.class.getName())

            .overrideConfigKey("quarkus.cxf.client.helloDefault.client-endpoint-url", "http://localhost:8081/services/hello")
            .overrideConfigKey("quarkus.cxf.client.helloDefault.service-interface", HelloService.class.getName())

            .overrideConfigKey("quarkus.cxf.client.helloUrlConnection.client-endpoint-url",
                    "http://localhost:8081/services/hello")
            .overrideConfigKey("quarkus.cxf.client.helloUrlConnection.service-interface", HelloService.class.getName())
            .overrideConfigKey("quarkus.cxf.client.helloUrlConnection.http-conduit-factory",
                    HTTPConduitImpl.URLConnectionHTTPConduitFactory.name())

            .overrideConfigKey("quarkus.cxf.client.helloHttpClient.client-endpoint-url", "http://localhost:8081/services/hello")
            .overrideConfigKey("quarkus.cxf.client.helloHttpClient.service-interface", HelloService.class.getName())
            .overrideConfigKey("quarkus.cxf.client.helloHttpClient.http-conduit-factory",
                    HTTPConduitImpl.HttpClientHTTPConduitFactory.name())

            .overrideConfigKey("quarkus.cxf.client.helloVertxClient.client-endpoint-url",
                    "http://localhost:8081/services/hello")
            .overrideConfigKey("quarkus.cxf.client.helloVertxClient.service-interface", HelloService.class.getName())
            .overrideConfigKey("quarkus.cxf.client.helloVertxClient.http-conduit-factory",
                    HTTPConduitImpl.VertxHttpClientHTTPConduitFactory.name());

    @CXFClient("helloDefault")
    HelloService helloDefault;

    @CXFClient("helloUrlConnection")
    HelloService helloUrlConnection;

    @CXFClient("helloHttpClient")
    HelloService helloHttpClient;

    @CXFClient("helloVertxClient")
    HelloService helloVertxClient;

    @Inject
    Logger logger;

    @Test
    void conduitFactory() {
        final Bus bus = BusFactory.getDefaultBus();
        final HTTPConduitFactory factory = bus.getExtension(HTTPConduitFactory.class);
        Assertions.assertThat(factory).isInstanceOf(VertxHttpClientHTTPConduitFactory.class);

        {
            final HelloService service = helloDefault;
            final Client client = ClientProxy.getClient(service);
            Assertions.assertThat(client.getConduit()).isInstanceOf(VertxHttpClientHTTPConduit.class);
            /* ... and make sure that the alternative conduit works */
            Assertions.assertThat(service.hello("Joe")).isEqualTo("Hello Joe");
        }
        {
            final HelloService service = helloUrlConnection;
            final Client client = ClientProxy.getClient(service);
            Assertions.assertThat(client.getConduit()).isInstanceOf(URLConnectionHTTPConduit.class);
            /* ... and make sure that the alternative conduit works */
            Assertions.assertThat(service.hello("Joe")).isEqualTo("Hello Joe");
        }
        {
            final HelloService service = helloHttpClient;
            final Client client = ClientProxy.getClient(service);
            Assertions.assertThat(client.getConduit()).isInstanceOf(HttpClientHTTPConduit.class);
            /* ... and make sure that the alternative conduit works */
            Assertions.assertThat(service.hello("Joe")).isEqualTo("Hello Joe");
        }
        {
            final HelloService service = helloVertxClient;
            final Client client = ClientProxy.getClient(service);
            Assertions.assertThat(client.getConduit()).isInstanceOf(VertxHttpClientHTTPConduit.class);
            /* ... and make sure that the alternative conduit works */
            Assertions.assertThat(service.hello("Joe")).isEqualTo("Hello Joe");
        }
    }

    @WebService
    public interface HelloService {

        @WebMethod
        String hello(String person);

    }

    @WebService(serviceName = "HelloService")
    public static class HelloServiceImpl implements HelloService {

        @Override
        public String hello(String person) {
            return "Hello " + person;
        }
    }

}
