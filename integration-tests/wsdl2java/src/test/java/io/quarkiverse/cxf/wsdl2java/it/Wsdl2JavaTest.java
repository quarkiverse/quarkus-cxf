package io.quarkiverse.cxf.wsdl2java.it;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.assertj.core.api.Assertions;
import org.jboss.eap.quickstarts.wscalculator.calculator.Add;
import org.jboss.eap.quickstarts.wscalculator.calculator.ObjectFactory;
import org.jboss.eap.quickstarts.wscalculator.calculator.Operands;
import org.junit.jupiter.api.Test;

public class Wsdl2JavaTest {

    @Test
    void codegenTests() throws IOException {

        /* Make sure that the java files were generated */
        final Path calculatorService_Service = Paths.get(
                "target/generated-test-sources/wsdl2java/org/jboss/eap/quickstarts/wscalculator/calculator/CalculatorService_Service.java");
        Assertions.assertThat(calculatorService_Service)
                .isRegularFile()
                .content(StandardCharsets.UTF_8).contains("wsdlLocation = \"classpath:wsdl/CalculatorService.wsdl\"");

    }

    @Test
    void generatedCodeCompiled() {
        /* Make sure that some of the generated classes can be loaded */
        ObjectFactory of = new ObjectFactory();
        Add add = of.createAdd();
        Assertions.assertThat(add).isNotNull();

    }

    @Test
    void toStringGenerated() {
        Operands op = new Operands();
        op.setA(1);
        op.setB(2);

        Assertions.assertThat(op.toString()).endsWith("[a=1,b=2]");
    }

}
