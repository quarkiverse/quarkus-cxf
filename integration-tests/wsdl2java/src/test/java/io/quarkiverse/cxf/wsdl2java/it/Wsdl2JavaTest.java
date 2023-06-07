package io.quarkiverse.cxf.wsdl2java.it;

import java.beans.PropertyChangeEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.jboss.eap.quickstarts.wscalculator.calculator.Add;
import org.jboss.eap.quickstarts.wscalculator.calculator.ObjectFactory;
import org.jboss.eap.quickstarts.wscalculator.calculator.Operands;
import org.jboss.eap.quickstarts.wscalculator.calculator.Result;
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

        Assertions.assertThat(op.toString()).contains("a=1,b=2");
    }

    @Test
    void propertyListenerGenerated() {
        Operands op = new Operands();
        List<PropertyChangeEvent> events = new ArrayList<>();
        op.addPropertyChangeListener(events::add);
        op.setA(1);
        op.setB(2);

        Assertions.assertThat(events).hasSize(2);
        PropertyChangeEvent e0 = events.get(0);
        Assertions.assertThat(e0.getPropertyName()).isEqualTo("a");
        Assertions.assertThat(e0.getNewValue()).isEqualTo(1);
        PropertyChangeEvent e1 = events.get(1);
        Assertions.assertThat(e1.getPropertyName()).isEqualTo("b");
        Assertions.assertThat(e1.getNewValue()).isEqualTo(2);
    }

    @Test
    void defaultGenerated() {
        Result result = new Result();

        Assertions.assertThat(result.getTheAnswer()).isEqualTo("42");
        result.setTheAnswer("43");
        Assertions.assertThat(result.getTheAnswer()).isEqualTo("43");
    }

    @Test
    void javadocGenerated() {
        /* Make sure that the java files were generated */
        final Path calculatorService_Service = Paths.get(
                "target/generated-test-sources/wsdl2java/org/jboss/eap/quickstarts/wscalculator/calculator/Result.java");
        Assertions.assertThat(calculatorService_Service)
                .isRegularFile()
                .content(StandardCharsets.UTF_8).contains("This text should appear in JavaDoc of result");
    }

    @Test
    void getBooleanGenerated() {
        /* Make sure that the java files were generated */
        final Path calculatorService_Service = Paths.get(
                "target/generated-test-sources/wsdl2java/org/jboss/eap/quickstarts/wscalculator/calculator/Result.java");
        Assertions.assertThat(calculatorService_Service)
                .isRegularFile()
                .content(StandardCharsets.UTF_8).contains("public boolean getEven() {");
    }

    @Test
    void extensibilityElementGenerated() {
        /* Make sure that the java files were generated */
        final Path calculatorService_Service = Paths.get(
                "target/generated-test-sources/wsdl2java/org/jboss/eap/quickstarts/wscalculator/calculator/Result.java");
        Assertions.assertThat(calculatorService_Service)
                .isRegularFile()
                .content(StandardCharsets.UTF_8).contains("implements ExtensibilityElement");
    }

}
