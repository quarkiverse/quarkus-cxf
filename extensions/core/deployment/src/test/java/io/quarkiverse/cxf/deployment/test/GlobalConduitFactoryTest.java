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
import org.assertj.core.api.Assertions;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.cxf.CxfClientConfig.HTTPConduitImpl;
import io.quarkiverse.cxf.HttpClientHTTPConduitFactory;
import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkus.test.QuarkusUnitTest;

public class GlobalConduitFactoryTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(HelloService.class, HelloServiceImpl.class))
            .overrideConfigKey("quarkus.cxf.endpoint.\"/hello\".implementor",
                    HelloServiceImpl.class.getName())
            .overrideConfigKey("quarkus.cxf.client.hello.client-endpoint-url", "http://localhost:8081/services/hello")
            .overrideConfigKey("quarkus.cxf.client.hello.service-interface", HelloService.class.getName())
            .overrideConfigKey("quarkus.cxf.http-conduit-factory", HTTPConduitImpl.HttpClientHTTPConduitFactory.name());

    @CXFClient
    HelloService helloService;

    @Inject
    Logger logger;

    @Test
    void conduitFactory() {
        final Bus bus = BusFactory.getDefaultBus();
        final HTTPConduitFactory factory = bus.getExtension(HTTPConduitFactory.class);
        Assertions.assertThat(factory).isInstanceOf(HttpClientHTTPConduitFactory.class);

        final Client client = ClientProxy.getClient(helloService);
        Assertions.assertThat(client.getConduit()).isInstanceOf(HttpClientHTTPConduit.class);

        /* ... and make sure that the alternative conduit works */
        Assertions.assertThat(helloService.hello("Joe")).isEqualTo("Hello Joe");
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
