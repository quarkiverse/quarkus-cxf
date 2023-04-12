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

public class DefaultConfigJava2WsTest {
    private static final String TEST_DIR = "java2ws/" + DefaultConfigJava2WsTest.class.getSimpleName();

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(GreetingWebService.class)
                    .addClass(FruitWebService.class)
                    .addClass(Fruit.class))
            /*
             * Make sure that we do not write to target/classes, because otherwise the WSDL could end up in the extension
             * deployment jar
             */
            .overrideConfigKey("quarkus.cxf.java2ws.wsdl-name-template",
                    "%TARGET_DIR%/" + TEST_DIR + "/%SIMPLE_CLASS_NAME%.wsdl");

    @Test
    public void generationTest() throws IOException {
        /* The default includes are empty so no WSDLs should have been generated */
        Assertions.assertThat(Path.of("target/" + TEST_DIR + "/GreetingWebService.wsdl")).doesNotExist();
        Assertions.assertThat(Path.of("target/" + TEST_DIR + "/FruitWebService.wsdl")).doesNotExist();
    }
}
