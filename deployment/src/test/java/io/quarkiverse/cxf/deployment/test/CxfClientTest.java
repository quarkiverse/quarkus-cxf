package io.quarkiverse.cxf.deployment.test;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class CxfClientTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(FruitWebService.class)
                    .addClass(Fruit.class))
            .withConfigurationResource("application-cxf-test.properties");

    //    @Inject
    //    @Named("io.quarkiverse.cxf.deployment.test.FruitWebService")
    //    CXFClientInfo clientInfo;

    @Inject
    FruitWebService clientService;

    //    @Inject
    //    @CXF(config = "fruit")
    //    FruitWebService clientServiceWithConfigFruit;

    @Test
    public void whenCheckingClientInjected() {
        Assertions.assertNotNull(clientService);
    }

    //    @Test
    //    public void whenCheckingClientInfoInjected() {
    //        Assertions.assertNotNull(clientInfo);
    //    }
    //
    //    @Test
    //    public void testClientInfoDetails() {
    //        Assertions.assertNotNull(clientInfo.getEndpointAddress());
    //    }

}
