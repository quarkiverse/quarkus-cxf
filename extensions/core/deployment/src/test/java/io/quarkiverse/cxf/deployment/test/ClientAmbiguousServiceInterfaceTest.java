package io.quarkiverse.cxf.deployment.test;

import jakarta.enterprise.inject.Instance;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * The build fails if a client is injected without a client name and there are multiple
 * {@code quarkus.cxf.client."client-name"} configurations having {@code service-interface} set to the service endpoint
 * interface of that client.
 */
public class ClientAmbiguousServiceInterfaceTest {

    private static final String SEI = FruitWebService.class.getName();

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(FruitWebService.class, Fruit.class, Add.class, Delete.class))
            .overrideConfigKey("quarkus.cxf.client.fruit1.client-endpoint-url", "http://localhost:8081/services/fruit")
            .overrideConfigKey("quarkus.cxf.client.fruit1.service-interface", SEI)
            .overrideConfigKey("quarkus.cxf.client.fruit2.client-endpoint-url", "http://localhost:8081/services/fruit")
            .overrideConfigKey("quarkus.cxf.client.fruit2.service-interface", SEI)
            .assertException(
                    t -> Assertions.assertThat(t).isInstanceOf(IllegalStateException.class)
                            .hasMessageContainingAll(
                                    "quarkus.cxf.client.\"client-name\".service-interface = " + SEI
                                            + " expected once, but found 2 times in ",
                                    "quarkus.cxf.client.\"fruit1\".service-interface",
                                    "quarkus.cxf.client.\"fruit2\".service-interface",
                                    "; either inject the client using an explicit client name,"
                                            + " e.g. @CXFClient(\"client-name\"),"
                                            + " or keep that service-interface only in one of those configurations"));

    @CXFClient
    Instance<FruitWebService> unnamed;

    @Test
    void ambiguous() {
        Assertions.fail("The application should not start");
    }
}
