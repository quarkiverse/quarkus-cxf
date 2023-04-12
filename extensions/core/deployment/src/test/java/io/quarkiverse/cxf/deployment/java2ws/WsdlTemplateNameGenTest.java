package io.quarkiverse.cxf.deployment.java2ws;

import java.io.IOException;
import java.nio.file.Path;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class WsdlTemplateNameGenTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(GreeterService.class)
                    .addClass(FruitWebService.class)
                    .addClass(Fruit.class))
            .overrideConfigKey("quarkus.cxf.codegen.wsdl2java.enabled", "false")
            .overrideConfigKey("quarkus.cxf.java2ws.enabled", "true")
            .overrideConfigKey("quarkus.cxf.java2ws.\"GS\".include", ".*GreeterService")
            .overrideConfigKey("quarkus.cxf.java2ws.\"GS\".output-dir", "target/classes/wsdl/WsdlTemplateNameGenTest")
            .overrideConfigKey("quarkus.cxf.java2ws.\"GS\".wsdl-name-template", "<CLASS_NAME>aaa<CLASS_NAME>.txt")
            .overrideConfigKey("quarkus.cxf.java2ws.\"FS\".include", ".*FruitWebService")
            .overrideConfigKey("quarkus.cxf.java2ws.\"FS\".output-dir", "target/classes/wsdl/WsdlTemplateNameGenTest")
            .overrideConfigKey("quarkus.cxf.java2ws.\"FS\".wsdl-name-template", "test_<FULLY_QUALIFIED_CLASS_NAME>.wsdl");

    @Test
    public void generationTest() throws IOException {
        Assertions
                .assertThat(Path.of("target/classes/wsdl/WsdlTemplateNameGenTest")
                        .resolve("GreeterServiceaaaGreeterService.txt").toFile().exists())
                .as("check Greeterservice.wsdl existence").isTrue();
        Assertions
                .assertThat(Path.of("target/classes/wsdl/WsdlTemplateNameGenTest")
                        .resolve("test_io_quarkiverse_cxf_deployment_java2ws_FruitWebService.wsdl").toFile().exists())
                .as("check FruitWebService.wsdl existence").isTrue();
    }
}
