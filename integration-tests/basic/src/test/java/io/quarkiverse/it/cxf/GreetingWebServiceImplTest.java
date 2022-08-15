package io.quarkiverse.it.cxf;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import java.net.MalformedURLException;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class GreetingWebServiceImplTest {
    private static GreetingWebService greetingWS;

    @BeforeAll
    public static void setup() throws MalformedURLException {
        greetingWS = ClientTestUtil.getClient(GreetingWebService.class);
    }

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
        String xml = "<x:Envelope xmlns:x=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:cxf=\"http://cxf.it.quarkiverse.io/\">\n"
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
                .when().post("/soap/greeting")
                .then()
                .statusCode(200)
                .body(containsString("Hello foo"));
    }

    @Test
    void soap12Binding() {
        given()
                .when().get("/soap/greeting?wsdl")
                .then()
                .statusCode(200)
                .body(
                        containsString("http://schemas.xmlsoap.org/wsdl/soap12/"),
                        containsString("<wsdl:portType name=\"GreetingWebService\">"),
                        containsString("<wsdl:operation name=\"reply\">"),
                        containsString("<wsdl:operation name=\"ping\">"));
    }

}
