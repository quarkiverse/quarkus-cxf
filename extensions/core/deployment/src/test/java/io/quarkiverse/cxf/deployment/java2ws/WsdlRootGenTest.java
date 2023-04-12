package io.quarkiverse.cxf.deployment.java2ws;

import java.io.IOException;
import java.nio.file.Path;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class WsdlRootGenTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(GreeterService.class)
                    .addClass(FruitWebService.class)
                    .addClass(Fruit.class))
            .overrideConfigKey("quarkus.cxf.codegen.wsdl2java.enabled", "false")
            .overrideConfigKey("quarkus.cxf.java2ws.enabled", "true")
            .overrideConfigKey("quarkus.cxf.java2ws.include", ".*")
            .overrideConfigKey("quarkus.cxf.java2ws.output-dir", "target/classes/wsdl/WsdlRootGenTest");

    @Test
    public void generationTest() throws IOException {
        Assertions.assertThat(Path.of("target/classes/wsdl/WsdlRootGenTest").resolve("GreeterService.wsdl").toFile().exists())
                .as("check Greeterservice.wsdl existence").isTrue();
        Assertions.assertThat(Path.of("target/classes/wsdl/WsdlRootGenTest").resolve("FruitWebService.wsdl").toFile().exists())
                .as("check FruitWebService.wsdl existence").isTrue();
    }
}
