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
                    .addClass(Fruit.class)
                    .addClass(GreetingWebService.class)
                    .addClass(GreetingWebServiceImpl.class)
            //.addClass(HelloWebServiceImpl.class)
            )
            .withConfigurationResource("application-cxf-test.properties");

    @Inject
    @Named("io.quarkiverse.cxf.deployment.test.FruitWebService")
    CXFClientInfo clientInfo;

    @Inject
    FruitWebService serviceImpl;

    @Inject
    @CXFClient
    FruitWebService proxyClient;

    @Test
    public void testInjectedBeansAvailable() {
        Assertions.assertNotNull(serviceImpl);
        Assertions.assertNotNull(clientInfo);
        Assertions.assertNotNull(proxyClient);

        Assertions.assertFalse(Proxy.isProxyClass(clientInfo.getClass()));
        Assertions.assertFalse(Proxy.isProxyClass(serviceImpl.getClass()));
        Assertions.assertTrue(Proxy.isProxyClass(proxyClient.getClass()));
    }

}
