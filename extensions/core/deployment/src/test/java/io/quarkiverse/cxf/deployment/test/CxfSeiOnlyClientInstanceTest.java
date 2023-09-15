package io.quarkiverse.cxf.deployment.test;

import java.lang.reflect.Proxy;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.cxf.CXFClientData;
import io.quarkiverse.cxf.CXFClientInfo;
import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkus.test.QuarkusUnitTest;

public class CxfSeiOnlyClientInstanceTest {

    private static final String FRUITCLIENT_URL = "http://localhost:8081/services/fruit";

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(FruitWebService.class)
                    .addClass(Fruit.class)
                    .addClass(Add.class)
                    .addClass(Delete.class)
                    .addClass(GreetingWebService.class))
            .overrideConfigKey("quarkus.cxf.client.fruitclient.client-endpoint-url", FRUITCLIENT_URL)
            .overrideConfigKey("quarkus.cxf.client.fruitclient.service-interface",
                    "io.quarkiverse.cxf.deployment.test.FruitWebService")
            .overrideConfigKey("quarkus.cxf.client.foo.client-endpoint-url", FRUITCLIENT_URL)
            .overrideConfigKey("quarkus.cxf.client.foo.service-interface",
                    "io.quarkiverse.cxf.deployment.test.FruitWebService")
            .overrideConfigKey("quarkus.cxf.client.foo.alternative", "true")
            .overrideConfigKey("quarkus.cxf.client.foo.features", "org.apache.cxf.feature.LoggingFeature");

    @Inject
    @CXFClient("fruitclient")
    Instance<CXFClientInfo> fruitInfoInstance;

    @Inject
    @CXFClient("foo")
    Instance<CXFClientInfo> fooInfoInstance;

    @Inject
    @Named("io.quarkiverse.cxf.deployment.test.FruitWebService")
    Instance<CXFClientData> clientDataInstance;

    @Inject
    @CXFClient
    Instance<FruitWebService> clientProxyinstance;

    @Test
    public void testInjectedInstances() {
        Assertions.assertThat(fruitInfoInstance.isResolvable()).isTrue();
        Assertions.assertThat(clientProxyinstance.isResolvable()).isTrue();

        final CXFClientInfo fruitClientInfo = fruitInfoInstance.get();
        Assertions.assertThat(fruitClientInfo).isNotNull();
        Assertions.assertThat(Proxy.isProxyClass(fruitClientInfo.getClass())).isFalse();
        Assertions.assertThat(fruitClientInfo.getEndpointAddress()).isEqualTo(FRUITCLIENT_URL);

        final CXFClientInfo fooClientInfo = fooInfoInstance.get();
        Assertions.assertThat(fooClientInfo).isNotNull();
        Assertions.assertThat(fooClientInfo.getEndpointAddress()).isEqualTo(FRUITCLIENT_URL);

        final FruitWebService clientProxy = clientProxyinstance.get();
        Assertions.assertThat(clientProxy).isNotNull();
        Assertions.assertThat(Proxy.isProxyClass(clientProxy.getClass())).isTrue();

        final CXFClientData cxfClientData = clientDataInstance.get();

        Assertions.assertThat(cxfClientData.getSei()).isEqualTo(fruitClientInfo.getSei());
        Assertions.assertThat(cxfClientData.getWsName()).isEqualTo(fruitClientInfo.getWsName());
        Assertions.assertThat(cxfClientData.getWsNamespace()).isEqualTo(fruitClientInfo.getWsNamespace());
        Assertions.assertThat(cxfClientData.getSoapBinding()).isEqualTo(fruitClientInfo.getSoapBinding());
        Assertions.assertThat(cxfClientData.isProxyClassRuntimeInitialized())
                .isEqualTo(fruitClientInfo.isProxyClassRuntimeInitialized());
    }

    @Test
    public void testUnusedSei() {
        Assertions.assertThat(CDI.current().getBeanManager().getBeans(GreetingWebService.class).isEmpty()).isTrue();
    }
}
