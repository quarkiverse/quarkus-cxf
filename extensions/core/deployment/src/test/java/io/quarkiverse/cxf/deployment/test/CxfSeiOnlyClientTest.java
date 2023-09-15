package io.quarkiverse.cxf.deployment.test;

import java.lang.reflect.Proxy;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.cxf.CXFClientData;
import io.quarkiverse.cxf.CXFClientInfo;
import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkus.test.QuarkusUnitTest;

public class CxfSeiOnlyClientTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(FruitWebService.class)
                    .addClass(Fruit.class)
                    .addClass(Add.class)
                    .addClass(Delete.class)
                    .addClass(GreetingWebService.class))
            .overrideConfigKey("quarkus.cxf.client.fruitclient.client-endpoint-url", "http://localhost:8081/services/fruit")
            .overrideConfigKey("quarkus.cxf.client.fruitclient.service-interface",
                    "io.quarkiverse.cxf.deployment.test.FruitWebService")
            .overrideConfigKey("quarkus.cxf.client.foo.client-endpoint-url", "http://localhost:8081/services/fruit")
            .overrideConfigKey("quarkus.cxf.client.foo.features", "org.apache.cxf.feature.LoggingFeature")
            .overrideConfigKey("quarkus.cxf.client.foo.alternative", "true")
            .overrideConfigKey("quarkus.cxf.client.foo.service-interface",
                    "io.quarkiverse.cxf.deployment.test.FruitWebService");

    @Inject
    @CXFClient("fruitclient")
    CXFClientInfo clientInfo;

    @Inject
    @Named("io.quarkiverse.cxf.deployment.test.FruitWebService")
    CXFClientData namedClientData;

    @Inject
    @CXFClient
    FruitWebService clientProxy;

    @Test
    public void testInjectedBeans() {
        Assertions.assertNotNull(clientInfo);
        Assertions.assertNotNull(clientProxy);

        Assertions.assertFalse(Proxy.isProxyClass(clientInfo.getClass()));
        Assertions.assertTrue(Proxy.isProxyClass(clientProxy.getClass()));

        Assertions.assertNotNull(namedClientData);
    }

    @Test
    public void testUnusedSei() {
        Assertions.assertTrue(CDI.current().getBeanManager().getBeans(GreetingWebService.class).isEmpty());
    }
}
