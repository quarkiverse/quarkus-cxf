package io.quarkiverse.cxf.deployment.java2ws;

import java.io.IOException;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class WsdlDisabledGenTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(GreeterService.class)
                    .addClass(FruitWebService.class)
                    .addClass(Fruit.class))
            .overrideConfigKey("quarkus.cxf.codegen.wsdl2java.enabled", "false")
            .overrideConfigKey("quarkus.cxf.java2ws.output-dir", "target/classes/wsdl/WsdlDisabledGenTest")
            .setLogRecordPredicate(lr -> lr.getMessage().contains("java2ws"))
            .assertLogRecords(
                    lrs -> {
                        if (!lrs.isEmpty()) {
                            Assertions.fail("There is java2ws execution.");
                        }
                    });

    @Test
    public void generationTest() throws IOException {
        //asserting log
    }
}
