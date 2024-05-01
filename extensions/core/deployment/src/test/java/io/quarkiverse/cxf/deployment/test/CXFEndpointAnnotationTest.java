package io.quarkiverse.cxf.deployment.test;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkiverse.cxf.annotation.CXFEndpoint;
import io.quarkus.test.QuarkusUnitTest;

public class CXFEndpointAnnotationTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(HelloService.class, PropertiesCounterInterceptor.class))
            .overrideConfigKey("quarkus.cxf.endpoint.\"/helloMockConfig\".in-interceptors", "#propertiesCounterInterceptor")

            .overrideConfigKey("quarkus.cxf.client.helloType.client-endpoint-url", "http://localhost:8081/services/helloType")
            .overrideConfigKey("quarkus.cxf.client.helloType.service-interface", HelloService.class.getName())

            .overrideConfigKey("quarkus.cxf.client.helloMock.client-endpoint-url", "http://localhost:8081/services/helloMock")
            .overrideConfigKey("quarkus.cxf.client.helloMock.service-interface", HelloService.class.getName())

            .overrideConfigKey("quarkus.cxf.client.helloMockConfig.client-endpoint-url",
                    "http://localhost:8081/services/helloMockConfig")
            .overrideConfigKey("quarkus.cxf.client.helloMockConfig.service-interface", HelloService.class.getName());

    @CXFClient("helloMock")
    HelloService helloMockClient;

    @CXFClient("helloMockConfig")
    HelloService helloMockConfigClient;

    @CXFClient("helloType")
    HelloService helloTypeClient;

    @Inject
    @Named("propertiesCounterInterceptor")
    PropertiesCounterInterceptor interceptor;

    @CXFEndpoint("/helloMock")
    HelloService helloMockService() {
        final HelloService result = Mockito.mock(HelloService.class);
        Mockito.when(result.hello("Mock")).thenReturn("helloMock!");
        return result;
    }

    @CXFEndpoint("/helloMockConfig")
    HelloService helloMockConfigService() {
        final HelloService result = Mockito.mock(HelloService.class);
        Mockito.when(result.hello("Mock")).thenReturn("helloMockConfig!");
        return result;
    }

    @Test
    void helloMock() {
        Assertions.assertThat(helloMockClient.hello("Mock")).isEqualTo("helloMock!");
    }

    /**
     * Make sure that the {@link CXFEndpoint} annotation combines well with application.properties.
     */
    @Test
    void helloMockConfig() {
        Assertions.assertThat(interceptor.counter.get()).isEqualTo(0);
        Assertions.assertThat(helloMockConfigClient.hello("Mock")).isEqualTo("helloMockConfig!");
        Assertions.assertThat(interceptor.counter.get()).isEqualTo(1);
    }

    @Test
    void helloType() {
        Assertions.assertThat(helloTypeClient.hello("Joe")).isEqualTo("Hello Joe from HelloTypeServiceImpl!");
    }

    @WebService
    public interface HelloService {

        @WebMethod
        String hello(String person);

    }

    @WebService(endpointInterface = "io.quarkiverse.cxf.deployment.test.CXFEndpointAnnotationTest$HelloService", serviceName = "HelloService")
    @CXFEndpoint("/helloType")
    public static class HelloTypeServiceImpl implements HelloService {
        @Override
        public String hello(String person) {
            return "Hello " + person + " from HelloTypeServiceImpl!";
        }
    }

    @Singleton
    @Named("propertiesCounterInterceptor")
    public static class PropertiesCounterInterceptor extends AbstractPhaseInterceptor<Message> {
        private final AtomicInteger counter = new AtomicInteger(0);

        public PropertiesCounterInterceptor() {
            super(Phase.RECEIVE);
        }

        @Override
        public void handleMessage(Message message) {
            counter.incrementAndGet();
        }

    }

}
