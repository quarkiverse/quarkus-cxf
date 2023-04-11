package io.quarkiverse.cxf.deployment.test;

import java.lang.reflect.Proxy;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class CxfClientConstructorInjectionTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(FruitWebService.class)
                    .addClass(Fruit.class)
                    .addClass(Add.class)
                    .addClass(Delete.class)
                    .addClass(CxfClientConstructorInjectionBean.class))
            .overrideConfigKey("quarkus.cxf.client.\"fruitclient\".client-endpoint-url", "http://localhost:8081/fruit")
            .overrideConfigKey("quarkus.cxf.client.\"fruitclient\".service-interface",
                    "io.quarkiverse.cxf.deployment.test.FruitWebService")
            .overrideConfigKey("quarkus.cxf.client.\"foo\".client-endpoint-url", "http://localhost:8081/fruit")
            .overrideConfigKey("quarkus.cxf.client.\"foo\".features", "org.apache.cxf.feature.LoggingFeature");

    @Inject
    CxfClientConstructorInjectionBean bean;

    @Test
    public void testInjectedBeans() {
        Assertions.assertNotNull(bean.getClientInfo());
        Assertions.assertNotNull(bean.getClientProxy());

        Assertions.assertFalse(Proxy.isProxyClass(bean.getClientInfo().getClass()));
        Assertions.assertTrue(Proxy.isProxyClass(bean.getClientProxy().getClass()));
    }

}
