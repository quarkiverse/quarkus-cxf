package io.quarkiverse.cxf.it.server;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

abstract class AbstractGreetingWebServiceTest {
    protected static GreetingWebService greetingWS;

    @Test
    void reply() {
        Assertions.assertThat(greetingWS.reply("bar")).isEqualTo("Hello bar");
    }

    @Test
    void ping() throws GreetingException {
        Assertions.assertThat(greetingWS.ping("foo")).isEqualTo("Hello foo");
    }

    @Test
    void greetingException() {
        Assertions.assertThatExceptionOfType(GreetingException.class)
                .isThrownBy(() -> greetingWS.ping("error"))
                .withMessage("foo");
    }

    @Test
    void rawSoap() {
        String xml = "<x:Envelope xmlns:x=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:cxf=\"http://server.it.cxf.quarkiverse.io/\">\n"
                +
                "   <x:Header/>\n" +
                "   <x:Body>\n" +
                "      <cxf:reply>\n" +
                "          <text>foo</text>\n" +
                "      </cxf:reply>\n" +
                "   </x:Body>\n" +
                "</x:Envelope>";

        given()
                .header("Content-Type", "text/xml").and().body(xml)
                .when().post(ClientTestUtil.getEndpointUrl(greetingWS))
                .then()
                .statusCode(200)
                .body(containsString("Hello foo"));
    }

    @Test
    void soap12Binding() {
        given()
                .when().get(ClientTestUtil.getEndpointUrl(greetingWS) + "?wsdl")
                .then()
                .statusCode(200)
                .body(
                        containsString("http://schemas.xmlsoap.org/wsdl/soap12/"),
                        containsString("<wsdl:portType name=\"" + getServiceInterface() + "\">"),
                        containsString("<wsdl:operation name=\"reply\">"),
                        containsString("<wsdl:operation name=\"ping\">"));
    }

    protected abstract String getServiceInterface();

}
