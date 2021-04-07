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
import io.quarkiverse.cxf.annotation.CXFImpl;
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
                    .addClass(HelloWebServiceImpl.class))
            .withConfigurationResource("application-cxf-test.properties");

    @Inject
    @Named("io.quarkiverse.cxf.deployment.test.FruitWebService")
    CXFClientInfo clientInfo;

    @Inject
    @CXFImpl
    FruitWebService serviceImpl;

    @Inject
    @Named("fruitclient")
    FruitWebService proxyClient;

    @Inject
    FruitWebService proxyClientB;

    @Inject
    @Named("io.quarkiverse.cxf.deployment.test.GreetingWebService")
    CXFClientInfo greetingInfo;

    @Inject
    @CXFImpl /* TODO: this work without */
    GreetingWebServiceImpl greetingImpl;

    @Inject
    @Named("greetingclient")
    GreetingWebService greetingClient;

    @Inject
    GreetingWebService greetingClientB;

    @Test
    public void test_injected_beans() {
        Assertions.assertNotNull(serviceImpl);
        Assertions.assertNotNull(clientInfo);
        Assertions.assertNotNull(proxyClient);
        Assertions.assertNotNull(proxyClientB);

        Assertions.assertFalse(Proxy.isProxyClass(clientInfo.getClass()));
        Assertions.assertFalse(Proxy.isProxyClass(serviceImpl.getClass()));
        Assertions.assertTrue(Proxy.isProxyClass(proxyClient.getClass()));
        Assertions.assertTrue(Proxy.isProxyClass(proxyClientB.getClass()));
    }

    @Test
    public void test_injected_greeting_beans() {
        Assertions.assertNotNull(greetingImpl);
        Assertions.assertNotNull(greetingInfo);
        Assertions.assertNotNull(greetingClient);
        Assertions.assertNotNull(greetingClientB);

        Assertions.assertFalse(Proxy.isProxyClass(greetingInfo.getClass()));
        Assertions.assertFalse(Proxy.isProxyClass(greetingImpl.getClass()));
        Assertions.assertTrue(Proxy.isProxyClass(greetingClient.getClass()));
        Assertions.assertTrue(Proxy.isProxyClass(greetingClientB.getClass()));
    }

}
