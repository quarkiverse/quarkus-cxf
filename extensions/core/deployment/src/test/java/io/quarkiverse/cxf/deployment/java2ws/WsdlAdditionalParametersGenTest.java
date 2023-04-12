package io.quarkiverse.cxf.deployment.java2ws;

import java.io.IOException;
import java.nio.file.Path;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class WsdlAdditionalParametersGenTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(GreeterService.class)
                    .addClass(FruitWebService.class)
                    .addClass(Fruit.class))
            .overrideConfigKey("quarkus.cxf.codegen.wsdl2java.enabled", "false")
            .overrideConfigKey("quarkus.cxf.java2ws.enabled", "true")
            .overrideConfigKey("quarkus.cxf.java2ws.output-dir", "target/classes/wsdl/WsdlAdditionalParametersGenTest")
            .overrideConfigKey("quarkus.cxf.java2ws.include", ".*")
            .overrideConfigKey("quarkus.cxf.java2ws.additional-params", "-portname,12345,-h")
            .setLogRecordPredicate(lr -> lr.getMessage().contains("-wsdl"))
            .assertLogRecords(
                    lrs -> {
                        if (!lrs.stream()
                                .anyMatch(logRecord -> logRecord.getMessage()
                                        .contains("-h io.quarkiverse.cxf.deployment.java2ws.GreeterService"))) {
                            Assertions.fail("There is no help message in the log.");
                        }
                    });

    @Test
    public void generationTest() throws IOException {
        //tool will show help and will not generate wsdl
        Assertions
                .assertThat(Path.of("target/classes/wsdl/WsdlAdditionalParametersGenTest").resolve("GreeterService.wsdl")
                        .toFile().exists())
                .as("check Greeterservice.wsdl existence").isFalse();
        Assertions
                .assertThat(Path.of("target/classes/wsdl/WsdlAdditionalParametersGenTest").resolve("FruitWebService.wsdl")
                        .toFile().exists())
                .as("check FruitWebService.wsdl existence").isFalse();

    }

}
