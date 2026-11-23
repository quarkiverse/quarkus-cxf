package io.quarkiverse.cxf.deployment.test;

import jakarta.inject.Inject;
import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Make sure that {@code proxy-configuration-name = none} makes the client ignore the default proxy configuration.
 */
public class ProxyConfigurationNoneTest {

    @RegisterExtension
    public static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(HelloService.class, HelloServiceImpl.class))
            .overrideConfigKey("quarkus.cxf.endpoint.\"/hello\".implementor", HelloServiceImpl.class.getName())

            // A default proxy that does not exist: any request going through it would fail
            .overrideConfigKey("quarkus.proxy.host", "localhost")
            .overrideConfigKey("quarkus.proxy.port", "1")

            .overrideConfigKey("quarkus.cxf.client.noProxy.client-endpoint-url", "http://localhost:8081/services/hello")
            .overrideConfigKey("quarkus.cxf.client.noProxy.service-interface", HelloService.class.getName())
            .overrideConfigKey("quarkus.cxf.client.noProxy.proxy-configuration-name", "none");

    @Inject
    @CXFClient("noProxy")
    HelloService noProxy;

    @Test
    void noProxy() {
        Assertions.assertThat(noProxy.hello("Joe")).isEqualTo("Hello Joe");
    }

    @WebService
    public interface HelloService {

        @WebMethod
        String hello(String person);
    }

    @WebService(endpointInterface = "io.quarkiverse.cxf.deployment.test.ProxyConfigurationNoneTest$HelloService", serviceName = "HelloService")
    public static class HelloServiceImpl implements HelloService {

        @Override
        public String hello(String person) {
            return "Hello " + person;
        }
    }
}
