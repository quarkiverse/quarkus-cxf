package io.quarkiverse.cxf.deployment.test;

import java.lang.reflect.Proxy;

import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.cxf.CXFClientInfo;
import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkus.test.QuarkusUnitTest;

public class CxfClientTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(FruitWebService.class)
                    .addClass(FruitWebServiceImpl.class)
                    .addClass(Fruit.class))
            .withConfigurationResource("application-cxf-test.properties");

    @Inject
    @Named("io.quarkiverse.cxf.deployment.test.FruitWebService")
    CXFClientInfo clientInfo;

    @Inject
    FruitWebService clientService;

    @Inject
    @CXFClient
    FruitWebService clientProxy;

    @Inject
    @CXFClient
    CXFClientInfo clientProxyInfo;

    @Inject
    @CXFClient("foo")
    CXFClientInfo fooInfo;

    @Test
    public void test_injected_beans() {
        Assertions.assertNotNull(clientService);
        Assertions.assertNotNull(clientInfo);
        Assertions.assertNotNull(clientProxy);
        Assertions.assertNotNull(clientProxyInfo);
        Assertions.assertNotNull(fooInfo);

        Assertions.assertFalse(Proxy.isProxyClass(clientInfo.getClass()));
        Assertions.assertFalse(Proxy.isProxyClass(clientService.getClass()));
        Assertions.assertTrue(Proxy.isProxyClass(clientProxy.getClass()));
        Assertions.assertFalse(Proxy.isProxyClass(clientProxyInfo.getClass()));
        Assertions.assertFalse(Proxy.isProxyClass(fooInfo.getClass()));
    }

    @Test
    public void test_injected_info() {
        Assertions.assertEquals("http://localhost:8081/fruit", fooInfo.getEndpointAddress());
        Assertions.assertEquals(FruitWebService.class.getName(), fooInfo.getSei());
        // check that "foo" has LoggingFeature ..
        Assertions.assertTrue(
                fooInfo.getFeatures().stream().anyMatch(feature -> feature.equals("org.apache.cxf.feature.LoggingFeature")));
    }
}
