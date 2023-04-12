package io.quarkiverse.cxf.deployment.java2ws;

import java.io.IOException;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class WsdlEmptyNamedIncludesGenTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(GreeterService.class)
                    .addClass(FruitWebService.class)
                    .addClass(Fruit.class))
            .overrideConfigKey("quarkus.cxf.codegen.wsdl2java.enabled", "false")
            .overrideConfigKey("quarkus.cxf.java2ws.enabled", "true")
            .overrideConfigKey("quarkus.cxf.java2ws.\"group_01\".output-dir",
                    "target/classes/wsdl/WsdlEmptyNamedIncludesGenTest")
            .setExpectedException(NullPointerException.class);

    @Test
    public void generationTest() throws IOException {
        Assertions.fail("Extension should not start");
    }
}
