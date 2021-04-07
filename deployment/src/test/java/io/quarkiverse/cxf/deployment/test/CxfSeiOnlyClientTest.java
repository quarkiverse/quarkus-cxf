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
import io.quarkus.test.QuarkusUnitTest;

public class CxfSeiOnlyClientTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(FruitWebService.class)
                    .addClass(Fruit.class))
            .withConfigurationResource("application-cxf-test.properties");

    @Inject
    @Named("io.quarkiverse.cxf.deployment.test.FruitWebService")
    CXFClientInfo clientInfo;

    @Inject
    @Named("whatever")
    FruitWebService clientProxy;

    @Inject
    @Named
    FruitWebService clientProxyB;

    @Inject
    FruitWebService clientProxyC;

    @Test
    public void test_injected_beans() {
        Assertions.assertNotNull(clientInfo);
        Assertions.assertNotNull(clientProxy);
        Assertions.assertNotNull(clientProxyB);
        Assertions.assertNotNull(clientProxyC);

        Assertions.assertFalse(Proxy.isProxyClass(clientInfo.getClass()));
        Assertions.assertTrue(Proxy.isProxyClass(clientProxy.getClass()));
        Assertions.assertTrue(Proxy.isProxyClass(clientProxyB.getClass()));
        Assertions.assertTrue(Proxy.isProxyClass(clientProxyC.getClass()));
    }

}
