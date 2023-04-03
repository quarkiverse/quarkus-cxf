package io.quarkiverse.cxf.deployment.test;

import java.lang.reflect.Proxy;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.cxf.CXFClientInfo;
import io.quarkus.test.QuarkusUnitTest;

public class CxfClientConstructorInjectionInstanceTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(FruitWebService.class)
                    .addClass(Fruit.class)
                    .addClass(Add.class)
                    .addClass(Delete.class)
                    .addClass(CxfClientConstructorInjectionInstanceBean.class))
            .withConfigurationResource("application-cxf-test.properties");

    @Inject
    CxfClientConstructorInjectionInstanceBean bean;

    @Test
    public void testInjectedInstances() {
        Instance<CXFClientInfo> clientInfoInstance = bean.getClientInfoInstance();
        Instance<FruitWebService> clientProxyInstance = bean.getClientProxyInstance();

        Assertions.assertNotNull(clientInfoInstance);
        Assertions.assertNotNull(clientProxyInstance);

        Assertions.assertTrue(clientInfoInstance.isResolvable());
        Assertions.assertTrue(clientProxyInstance.isResolvable());

        Assertions.assertFalse(Proxy.isProxyClass(clientInfoInstance.get().getClass()));
        Assertions.assertTrue(Proxy.isProxyClass(clientProxyInstance.get().getClass()));
    }
}
