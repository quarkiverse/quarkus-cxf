package io.quarkiverse.cxf.it.validation;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkiverse.cxf.it.server.xml.schema.validation.model.CalculatorService;
import io.quarkiverse.cxf.test.QuarkusCxfClientTestUtil;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class XmlSchemaValidationTest {

    @Test
    public void addCheckParametersValid() {
        /*
         * Both operands fulfill the <= 1024 constraint of the smallIntType type defined in calculator.xsd
         * and the return value is a valid int
         */
        Assertions.assertThat(getClient().addCheckParameters(1024, 1024)).isEqualTo(2048);
    }

    @Test
    public void addCheckParametersInvalid() {
        /*
         * The first operand is not a valid value for the smallIntType type defined in calculator.xsd
         * thus we expect and error
         */
        Assertions.assertThatThrownBy(() -> getClient().addCheckParameters(1028, 2))
                .hasRootCauseInstanceOf(org.apache.cxf.binding.soap.SoapFault.class)
                .rootCause()
                .hasMessageContaining(
                        "Unmarshalling Error: cvc-maxInclusive-valid: Value '1028' is not facet-valid with respect to maxInclusive '1024' for type 'smallIntType'.");

        ;
    }

    @Test
    public void addCheckResultValid() {
        /*
         * Both operands are valid integers
         * and the return value still fulfills the <= 1024 constraint of the smallIntType type defined in calculator.xsd
         */
        Assertions.assertThat(getClient().addCheckResult(1028, -4)).isEqualTo(1024);
    }

    @Test
    public void addCheckResultInvalid() {
        /*
         * Both operands are valid integers
         * but the return value 1025 is not a valid value for the smallIntType (<= 1024) type defined in calculator.xsd
         * thus we expect and error
         */
        Assertions.assertThatThrownBy(() -> getClient().addCheckResult(1024, 1))
                .hasRootCauseInstanceOf(org.apache.cxf.binding.soap.SoapFault.class)
                .rootCause()
                .hasMessageContaining(
                        "Marshalling Error: cvc-maxInclusive-valid: Value '1025' is not facet-valid with respect to maxInclusive '1024' for type 'smallIntType'.");
    }

    private CalculatorService getClient() {
        final CalculatorService client = QuarkusCxfClientTestUtil.getClient(
                "http://www.jboss.org/eap/quickstarts/wscalculator/Calculator",
                CalculatorService.class,
                "/soap/schema-validated-calculator");
        return client;
    }

}
