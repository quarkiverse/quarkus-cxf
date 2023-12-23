package io.quarkiverse.cxf.it.validation;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.SoapFault;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkiverse.cxf.it.server.xml.schema.validation.model.CalculatorService;
import io.quarkiverse.cxf.test.QuarkusCxfClientTestUtil;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

@QuarkusTest
public class XmlSchemaValidationTest {

    @ParameterizedTest
    @ValueSource(strings = { "/annotation-schema-validated-calculator", "/application-properties-schema-validated-calculator",
            "/client-server/validation" })
    public void addCheckParametersValid(String endpoint) {
        /*
         * Both operands fulfill the <= 1024 constraint of the smallIntType type defined in calculator.xsd
         * and the return value is a valid int
         */
        Assertions.assertThat(getClient(endpoint).addCheckParameters(1024, 1024)).isEqualTo(2048);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/annotation-schema-validated-calculator", "/application-properties-schema-validated-calculator",
            "/client-server/validation"

    })
    public void addCheckParametersInvalid(String endpoint) {
        /*
         * The first operand is not a valid value for the smallIntType type defined in calculator.xsd
         * thus we expect and error
         */
        Assertions.assertThatThrownBy(() -> getClient(endpoint).addCheckParameters(1028, 2))
                .hasRootCauseInstanceOf(org.apache.cxf.binding.soap.SoapFault.class)
                .rootCause()
                .hasMessageContaining(
                        /* [M|Unm] */"arshalling Error: cvc-maxInclusive-valid: Value '1028' is not facet-valid with respect to maxInclusive '1024' for type 'smallIntType'.");

        ;
    }

    @ParameterizedTest
    @ValueSource(strings = { "/annotation-schema-validated-calculator", "/application-properties-schema-validated-calculator",
            "/client-server/validation" })
    public void addCheckResultValid(String endpoint) {
        /*
         * Both operands are valid integers
         * and the return value still fulfills the <= 1024 constraint of the smallIntType type defined in calculator.xsd
         */
        Assertions.assertThat(getClient(endpoint).addCheckResult(1028, -4)).isEqualTo(1024);
    }

    @ParameterizedTest
    @ValueSource(strings = { "/annotation-schema-validated-calculator", "/application-properties-schema-validated-calculator",
            "/client-server/validation" })
    public void addCheckResultInvalid(String endpoint) {
        /*
         * Both operands are valid integers
         * but the return value 1025 is not a valid value for the smallIntType (<= 1024) type defined in calculator.xsd
         * thus we expect and error
         */
        Assertions.assertThatThrownBy(() -> getClient(endpoint).addCheckResult(1024, 1))
                .hasRootCauseInstanceOf(org.apache.cxf.binding.soap.SoapFault.class)
                .rootCause()
                .hasMessageContaining(
                        /* [M|Unm] */"arshalling Error: cvc-maxInclusive-valid: Value '1025' is not facet-valid with respect to maxInclusive '1024' for type 'smallIntType'.");
    }

    private CalculatorService getClient(String endpoint) {
        if (endpoint.equals("/client-server/validation")) {
            return new RestWrapper();
        }
        final CalculatorService client = QuarkusCxfClientTestUtil.getClient(
                "http://www.jboss.org/eap/quickstarts/wscalculator/Calculator",
                CalculatorService.class,
                "/soap" + endpoint);
        return client;
    }

    private static class RestWrapper implements CalculatorService {

        @Override
        public int addCheckResult(int a, int b) {
            return get("addCheckResult", a, b);
        }

        private int get(String method, int a, int b) {
            ExtractableResponse<Response> response = RestAssured.given()
                    .queryParam("a", a)
                    .queryParam("b", b)
                    .get("/client-server/validation/" + method)
                    .then().extract();

            if (response.statusCode() == 200) {
                return Integer.parseInt(response.body().asString());
            } else {
                throw new RuntimeException(new SoapFault(response.body().asString(), new QName("error")));
            }
        }

        @Override
        public int addCheckParameters(int a, int b) {
            return get("addCheckParameters", a, b);
        }

    }

}
