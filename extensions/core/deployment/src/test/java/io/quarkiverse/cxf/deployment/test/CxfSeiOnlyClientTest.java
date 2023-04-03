package io.quarkiverse.cxf.deployment.test;

import java.lang.reflect.Proxy;

import javax.enterprise.inject.spi.CDI;
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

public class CxfSeiOnlyClientTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(FruitWebService.class)
                    .addClass(Fruit.class)
                    .addClass(Add.class)
                    .addClass(Delete.class)
                    .addClass(GreetingWebService.class))
            .withConfigurationResource("application-cxf-test.properties");

    @Inject
    CXFClientInfo clientInfo;

    @Inject
    @Named("io.quarkiverse.cxf.deployment.test.FruitWebService")
    CXFClientInfo namedClientInfo;

    @Inject
    @CXFClient
    FruitWebService clientProxy;

    @Test
    public void testInjectedBeans() {
        Assertions.assertNotNull(clientInfo);
        Assertions.assertNotNull(clientProxy);

        Assertions.assertFalse(Proxy.isProxyClass(clientInfo.getClass()));
        Assertions.assertTrue(Proxy.isProxyClass(clientProxy.getClass()));

        Assertions.assertEquals(namedClientInfo, clientInfo);
    }

    @Test
    public void testUnusedSei() {
        Assertions.assertTrue(CDI.current().getBeanManager().getBeans(GreetingWebService.class).isEmpty());
    }
}
