package io.quarkiverse.cxf.deployment.test;

import java.lang.reflect.Proxy;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.cxf.CXFClientInfo;
import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkus.test.QuarkusUnitTest;

public class CxfSeiOnlyClientInstanceTest {

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
    Instance<CXFClientInfo> clientInfoInstance;

    @Inject
    @Named("io.quarkiverse.cxf.deployment.test.FruitWebService")
    Instance<CXFClientInfo> namedClientInfoInstance;

    @Inject
    @CXFClient
    Instance<FruitWebService> clientProxyinstance;

    @Test
    public void testInjectedInstances() {
        Assertions.assertTrue(clientInfoInstance.isResolvable());
        Assertions.assertTrue(clientProxyinstance.isResolvable());

        CXFClientInfo clientInfo = clientInfoInstance.get();
        FruitWebService clientProxy = clientProxyinstance.get();

        Assertions.assertNotNull(clientInfo);
        Assertions.assertNotNull(clientProxy);

        Assertions.assertFalse(Proxy.isProxyClass(clientInfo.getClass()));
        Assertions.assertTrue(Proxy.isProxyClass(clientProxy.getClass()));

        Assertions.assertEquals(namedClientInfoInstance.get(), clientInfo);
    }

    @Test
    public void testUnusedSei() {
        Assertions.assertTrue(CDI.current().getBeanManager().getBeans(GreetingWebService.class).isEmpty());
    }
}
