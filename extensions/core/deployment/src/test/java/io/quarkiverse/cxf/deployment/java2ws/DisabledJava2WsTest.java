package io.quarkiverse.cxf.deployment.java2ws;

import java.io.IOException;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.cxf.deployment.test.Fruit;
import io.quarkiverse.cxf.deployment.test.FruitWebService;
import io.quarkiverse.cxf.deployment.test.GreetingWebService;
import io.quarkus.test.QuarkusUnitTest;

public class DisabledJava2WsTest {
    private static final String TEST_DIR = "java2ws/" + DisabledJava2WsTest.class.getSimpleName();

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(GreetingWebService.class)
                    .addClass(FruitWebService.class)
                    .addClass(Fruit.class))
            .overrideConfigKey("quarkus.java2ws.enabled", "false")
            /*
             * Make sure that we do not write to target/classes, because otherwise the WSDL could end up in the extension
             * deployment jar
             */
            .overrideConfigKey("quarkus.cxf.java2ws.wsdl-name-template",
                    "%TARGET_DIR%/" + TEST_DIR + "/%SIMPLE_CLASS_NAME%.wsdl")
            .setLogRecordPredicate(lr -> lr.getMessage().contains("Running java2ws"))
            .assertLogRecords(
                    lrs -> {
                        if (!lrs.isEmpty()) {
                            Assertions.fail("There is java2ws execution: "
                                    + lrs.stream().map(LogRecord::getMessage).collect(Collectors.joining("\n")));
                        }
                    });

    @Test
    public void generationTest() throws IOException {
        //asserting log
    }
}
