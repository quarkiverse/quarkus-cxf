package io.quarkiverse.cxf.deployment.test;

import jakarta.inject.Inject;
import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.transport.http.HTTPConduitFactory;
import org.apache.cxf.transport.http.URLConnectionHTTPConduit;
import org.assertj.core.api.Assertions;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.cxf.HTTPConduitImpl;
import io.quarkiverse.cxf.URLConnectionHTTPConduitFactory;
import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkiverse.cxf.deployment.test.ClientConduitFactoryTest.HelloService;
import io.quarkiverse.cxf.vertx.http.client.VertxHttpClientHTTPConduit;
import io.quarkus.test.QuarkusUnitTest;

public class GlobalConduitFactoryTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(HelloService.class, HelloServiceImpl.class))
            .overrideConfigKey("quarkus.cxf.endpoint.\"/hello\".implementor",
                    HelloServiceImpl.class.getName())
            .overrideConfigKey("quarkus.cxf.client.helloGlobal.client-endpoint-url", "http://localhost:8081/services/hello")
            .overrideConfigKey("quarkus.cxf.client.helloGlobal.service-interface", HelloService.class.getName())

            .overrideConfigKey("quarkus.cxf.client.helloVertxClient.client-endpoint-url",
                    "http://localhost:8081/services/hello")
            .overrideConfigKey("quarkus.cxf.client.helloVertxClient.service-interface", HelloService.class.getName())
            .overrideConfigKey("quarkus.cxf.client.helloVertxClient.http-conduit-factory",
                    HTTPConduitImpl.VertxHttpClientHTTPConduitFactory.name())

            .overrideConfigKey("quarkus.cxf.http-conduit-factory", HTTPConduitImpl.URLConnectionHTTPConduitFactory.name());

    @CXFClient("helloGlobal")
    HelloService helloGlobal;

    @CXFClient("helloVertxClient")
    HelloService helloVertxClient;

    @Inject
    Logger logger;

    @Test
    void conduitFactory() {
        final Bus bus = BusFactory.getDefaultBus();
        final HTTPConduitFactory factory = bus.getExtension(HTTPConduitFactory.class);
        Assertions.assertThat(factory).isNull();

        {
            final Client client = ClientProxy.getClient(helloGlobal);
            Assertions.assertThat(client.getConduit()).isInstanceOf(URLConnectionHTTPConduit.class);
            /* ... and make sure that the alternative conduit works */
            Assertions.assertThat(helloGlobal.hello("Joe")).isEqualTo("Hello Joe");
        }

        {
            final Client client = ClientProxy.getClient(helloVertxClient);
            Assertions.assertThat(client.getConduit()).isInstanceOf(VertxHttpClientHTTPConduit.class);
            /* ... and make sure that the alternative conduit works */
            Assertions.assertThat(helloVertxClient.hello("Joe")).isEqualTo("Hello Joe");
        }
    }

    @WebService
    public interface HelloService {

        @WebMethod
        String hello(String person);

    }

    @WebService(endpointInterface = "io.quarkiverse.cxf.deployment.test.GlobalConduitFactoryTest$HelloService", serviceName = "HelloService")
    public static class HelloServiceImpl implements HelloService {

        @Override
        public String hello(String person) {
            return "Hello " + person;
        }
    }

}
