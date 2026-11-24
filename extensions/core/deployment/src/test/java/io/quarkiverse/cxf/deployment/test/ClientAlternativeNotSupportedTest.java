package io.quarkiverse.cxf.deployment.test;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * The build fails if {@code quarkus.cxf.client."client-name".alternative} is set to any value, including {@code false},
 * even if there is no injection point for the given client.
 */
public class ClientAlternativeNotSupportedTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .overrideConfigKey("quarkus.cxf.client.foo.alternative", "false")
            .overrideConfigKey("quarkus.cxf.client.bar.alternative", "true")
            .overrideConfigKey("quarkus.cxf.client.baz.client-endpoint-url", "http://localhost:8081/services/baz")
            .assertException(
                    t -> Assertions.assertThat(t).isInstanceOf(IllegalStateException.class)
                            .hasMessage(
                                    "quarkus.cxf.client.\"client-name\".alternative is not supported since Quarkus CXF 4.0.0,"
                                            + " but found quarkus.cxf.client.\"bar\".alternative, quarkus.cxf.client.\"foo\".alternative;"
                                            + " remove it and either inject the client using an explicit client name,"
                                            + " e.g. @CXFClient(\"client-name\"), or make sure that there is only one"
                                            + " quarkus.cxf.client.\"client-name\" having service-interface set to the service"
                                            + " endpoint interface of the client"));

    @Test
    void alternativeSet() {
        Assertions.fail("The application should not start");
    }
}
