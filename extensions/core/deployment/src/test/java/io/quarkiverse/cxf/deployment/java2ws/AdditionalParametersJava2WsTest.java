package io.quarkiverse.cxf.deployment.java2ws;

import java.io.IOException;
import java.nio.file.Path;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.cxf.deployment.test.Fruit;
import io.quarkiverse.cxf.deployment.test.FruitWebService;
import io.quarkiverse.cxf.deployment.test.GreetingWebService;
import io.quarkus.test.QuarkusUnitTest;

public class AdditionalParametersJava2WsTest {
    private static final String TEST_DIR = "java2ws/" + AdditionalParametersJava2WsTest.class.getSimpleName();

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(GreetingWebService.class)
                    .addClass(FruitWebService.class)
                    .addClass(Fruit.class))
            .overrideConfigKey("quarkus.cxf.java2ws.includes", "**.*WebService")
            .overrideConfigKey("quarkus.cxf.java2ws.additional-params", "-portname,12345,-h")
            /*
             * Make sure that we do not write to target/classes, because otherwise the WSDL could end up in the extension
             * deployment jar
             */
            .overrideConfigKey("quarkus.cxf.java2ws.wsdl-name-template",
                    "%TARGET_DIR%/" + TEST_DIR + "/%SIMPLE_CLASS_NAME%.wsdl")
            .setLogRecordPredicate(lr -> lr.getMessage().contains("-wsdl")).assertLogRecords(lrs -> {
                if (!lrs.stream()
                        .anyMatch(logRecord -> logRecord.getMessage()
                                .contains("-h " + FruitWebService.class.getName()))) {
                    Assertions.fail("There is no help message in the log.");
                }
            });

    @Test
    public void generationTest() throws IOException {
        // java2ws shows help but does not generate any WSDLs
        Assertions.assertThat(Path.of("target/" + TEST_DIR + "/GreetingWebService.wsdl")).doesNotExist();
        Assertions.assertThat(Path.of("target/" + TEST_DIR + "/FruitWebService.wsdl")).doesNotExist();
    }

}
