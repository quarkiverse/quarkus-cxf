package io.quarkiverse.cxf.deployment.java2ws;

import java.io.IOException;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.cxf.deployment.test.Fruit;
import io.quarkiverse.cxf.deployment.test.FruitWebService;
import io.quarkiverse.cxf.deployment.test.GreetingWebService;
import io.quarkus.test.QuarkusUnitTest;

public class ExcludesWithoutIncludesJava2WsTest {
    private static final String TEST_DIR = "java2ws/" + ExcludesWithoutIncludesJava2WsTest.class.getSimpleName();

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(GreetingWebService.class)
                    .addClass(FruitWebService.class)
                    .addClass(Fruit.class))
            .overrideConfigKey("quarkus.cxf.java2ws.group_01.excludes", "foo")
            /*
             * Make sure that we do not write to target/classes, because otherwise the WSDL could end up in the extension
             * deployment jar
             */
            .overrideConfigKey("quarkus.cxf.java2ws.group_01.wsdl-name-template",
                    "%TARGET_DIR%/" + TEST_DIR + "/%SIMPLE_CLASS_NAME%.wsdl")
            .assertException(
                    t -> Assertions.assertThat(t).isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("Specify some includes"));

    @Test
    public void generationTest() throws IOException {
        Assertions.fail("Extension should not start");
    }
}
