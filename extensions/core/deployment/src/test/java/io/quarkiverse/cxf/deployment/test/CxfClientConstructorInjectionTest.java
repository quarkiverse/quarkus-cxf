package io.quarkiverse.cxf.deployment.test;

import java.lang.reflect.Proxy;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.cxf.CXFClientInfo;
import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkus.test.QuarkusUnitTest;

public class CxfClientConstructorInjectionTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(FruitWebService.class)
                    .addClass(Fruit.class)
                    .addClass(Add.class)
                    .addClass(Delete.class))
            .overrideConfigKey("quarkus.cxf.client.\"fruitclient\".client-endpoint-url", "http://localhost:8081/services/fruit")
            .overrideConfigKey("quarkus.cxf.client.\"fruitclient\".service-interface",
                    "io.quarkiverse.cxf.deployment.test.FruitWebService")
            .overrideConfigKey("quarkus.cxf.client.\"foo\".client-endpoint-url", "http://localhost:8081/services/fruit")
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

    @ApplicationScoped
    public static class CxfClientConstructorInjectionBean {

        private final CXFClientInfo clientInfo;
        private final FruitWebService clientProxy;

        @Inject
        public CxfClientConstructorInjectionBean(
                @CXFClient("fruitclient") CXFClientInfo clientInfo,
                @CXFClient FruitWebService clientProxy) {
            this.clientInfo = clientInfo;
            this.clientProxy = clientProxy;
        }

        public CXFClientInfo getClientInfo() {
            return clientInfo;
        }

        public FruitWebService getClientProxy() {
            return clientProxy;
        }
    }

}
