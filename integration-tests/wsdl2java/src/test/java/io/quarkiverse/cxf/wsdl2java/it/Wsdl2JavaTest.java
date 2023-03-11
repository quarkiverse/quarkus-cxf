package io.quarkiverse.cxf.wsdl2java.it;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.assertj.core.api.Assertions;
import org.jboss.eap.quickstarts.wscalculator.calculator.Add;
import org.jboss.eap.quickstarts.wscalculator.calculator.ObjectFactory;
import org.junit.jupiter.api.Test;

public class Wsdl2JavaTest {

    @Test
    void codegenTests() throws IOException {

        /* Make sure that the java files were generated */
        final Path javaFile = Paths.get(
                "target/generated-test-sources/wsdl2java/org/jboss/eap/quickstarts/wscalculator/calculator/ObjectFactory.java");
        Assertions.assertThat(javaFile).isRegularFile();

        /* Make sure that some of the generated classes can be loaded */
        ObjectFactory of = new ObjectFactory();
        Add add = of.createAdd();
        Assertions.assertThat(add).isNotNull();
    }

}
