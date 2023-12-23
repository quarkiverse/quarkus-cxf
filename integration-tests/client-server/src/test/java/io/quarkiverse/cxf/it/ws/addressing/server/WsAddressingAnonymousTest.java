package io.quarkiverse.cxf.it.ws.addressing.server;

import static io.quarkiverse.cxf.test.QuarkusCxfClientTestUtil.anyNs;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;

@QuarkusTest
public class WsAddressingAnonymousTest {
    static final String SOAP_REQUEST = "<x:Envelope xmlns:x=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:cxf=\"http://anonymous.server.addressing.ws.it.cxf.quarkiverse.io/\">\n"
            +
            "   <x:Header/>\n" +
            "   <x:Body>\n" +
            "      <cxf:reply>\n" +
            "          <text>foo</text>\n" +
            "      </cxf:reply>\n" +
            "   </x:Body>\n" +
            "</x:Envelope>";

    @Test
    void wsdl() {
        RestAssuredConfig config = RestAssured.config();
        config.getXmlConfig().namespaceAware(false);
        given()
                .config(config)
                .when().get("/soap/addressing-anonymous?wsdl")
                .then()
                .statusCode(200)
                .body(
                        Matchers.hasXPath(
                                anyNs("definitions", "binding", "UsingAddressing")
                                        + "/@*[local-name() = 'required']",
                                CoreMatchers.is("true")),
                        Matchers.hasXPath(
                                "local-name(" + anyNs("definitions", "Policy", "Addressing") + ")",
                                CoreMatchers.is("Addressing")));
    }

    @Test
    void rawSoap() {
        // The service has @Addressing(required = true) so sending a message without addressing headers must fail
        given()
                .header("Content-Type", "text/xml").and().body(SOAP_REQUEST)
                .when().post("/soap/addressing-anonymous")
                .then()
                .statusCode(500)
                .body(containsString("A required header representing a Message Addressing Property is not present"));
    }

    @Test
    void rawAddressing() {
        final String ID = "urn:uuid:50784fd1-f67e-4493-b24c-5850fb38736f";
        final String request = "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\">\n"
                + "  <soap:Header>\n"
                + "    <Action xmlns=\"http://www.w3.org/2005/08/addressing\" soap:mustUnderstand=\"true\">http://anonymous.server.addressing.ws.it.cxf.quarkiverse.io/GreetingWebServiceAddressingImpl/replyRequest</Action>\n"
                + "    <MessageID xmlns=\"http://www.w3.org/2005/08/addressing\" soap:mustUnderstand=\"true\">" + ID
                + "</MessageID>\n"
                + "    <To xmlns=\"http://www.w3.org/2005/08/addressing\" soap:mustUnderstand=\"true\">http://localhost:8081/soap/greeting-addressing</To>\n"
                + "    <ReplyTo xmlns=\"http://www.w3.org/2005/08/addressing\" soap:mustUnderstand=\"true\">\n"
                + "      <Address>http://www.w3.org/2005/08/addressing/anonymous</Address>\n"
                + "    </ReplyTo>\n"
                + "  </soap:Header>\n"
                + "  <soap:Body>\n"
                + "    <ns2:reply xmlns:ns2=\"http://anonymous.server.addressing.ws.it.cxf.quarkiverse.io/\">\n"
                + "      <text>bar</text>\n"
                + "    </ns2:reply>\n"
                + "  </soap:Body>\n"
                + "</soap:Envelope>";

        given()
                .header("Content-Type", "text/xml").and().body(request)
                .when().post("/soap/addressing-anonymous")
                .then()
                .statusCode(200)
                .body(
                        Matchers.hasXPath(
                                anyNs("Envelope", "Header", "Action") + "/text()",
                                CoreMatchers.is(
                                        "http://anonymous.server.addressing.ws.it.cxf.quarkiverse.io/AddressingAnonymousImpl/replyResponse")),
                        Matchers.hasXPath(
                                anyNs("Envelope", "Header", "MessageID") + "/text()",
                                Matchers.notNullValue(String.class)),
                        Matchers.hasXPath(
                                anyNs("Envelope", "Header", "To") + "/text()",
                                CoreMatchers.is("http://www.w3.org/2005/08/addressing/anonymous")),
                        Matchers.hasXPath(
                                anyNs("Envelope", "Header", "RelatesTo") + "/text()",
                                CoreMatchers.is(ID)));
    }

}
