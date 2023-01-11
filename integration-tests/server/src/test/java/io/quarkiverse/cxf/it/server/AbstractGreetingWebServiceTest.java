package io.quarkiverse.cxf.it.server;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkiverse.cxf.test.QuarkusCxfClientTestUtil;

abstract class AbstractGreetingWebServiceTest {
    protected static GreetingWebService greetingWS;
    static final String SOAP_REQUEST = "<x:Envelope xmlns:x=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:cxf=\"http://server.it.cxf.quarkiverse.io/\">\n"
            +
            "   <x:Header/>\n" +
            "   <x:Body>\n" +
            "      <cxf:reply>\n" +
            "          <text>foo</text>\n" +
            "      </cxf:reply>\n" +
            "   </x:Body>\n" +
            "</x:Envelope>";

    @Test
    void reply() {
        Assertions.assertThat(greetingWS.reply("bar")).isEqualTo("Hello bar");
    }

    @Test
    void rawSoap() {
        given()
                .header("Content-Type", "text/xml").and().body(SOAP_REQUEST)
                .when().post(QuarkusCxfClientTestUtil.getEndpointUrl(greetingWS))
                .then()
                .statusCode(200)
                .body(containsString("Hello foo"));
    }

    @Test
    void soap12Binding() {
        given()
                .when().get(QuarkusCxfClientTestUtil.getEndpointUrl(greetingWS) + "?wsdl")
                .then()
                .statusCode(200)
                .body(
                        containsString("http://schemas.xmlsoap.org/wsdl/soap12/"),
                        containsString("<wsdl:portType name=\"" + getServiceInterface() + "\">"),
                        containsString("<wsdl:operation name=\"reply\">"));
    }

    protected abstract String getServiceInterface();

}
