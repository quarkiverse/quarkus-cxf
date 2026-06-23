package io.quarkiverse.cxf.it.ws.addressing.server;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkiverse.cxf.it.HelloService;
import io.quarkiverse.cxf.test.internal.QuarkusCxfInternalTestUtil;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;

@QuarkusTest
public class WsAddressingAnonymousTest {
    static final String SOAP_REQUEST = """
            <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
              <soap:Header/>
              <soap:Body>
                <ns2:hello xmlns:ns2="https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test">
                  <arg0>Joe</arg0>
                </ns2:hello>
              </soap:Body>
            </soap:Envelope>
            """;

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
                                QuarkusCxfInternalTestUtil.anyNs("definitions", "binding", "UsingAddressing")
                                        + "/@*[local-name() = 'required']",
                                CoreMatchers.is("true")),
                        Matchers.hasXPath(
                                "local-name(" + QuarkusCxfInternalTestUtil.anyNs("definitions", "Policy", "Addressing") + ")",
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
        final String request = """
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                  <soap:Header>
                    <Action xmlns="http://www.w3.org/2005/08/addressing" soap:mustUnderstand="1">helloAction</Action>
                    <MessageID xmlns="http://www.w3.org/2005/08/addressing" soap:mustUnderstand="1">%s</MessageID>
                    <To xmlns="http://www.w3.org/2005/08/addressing" soap:mustUnderstand="1">http://localhost:8081/soap/addressing-headers-enforcer</To>
                    <ReplyTo xmlns="http://www.w3.org/2005/08/addressing" soap:mustUnderstand="1">
                      <Address>http://www.w3.org/2005/08/addressing/anonymous</Address>
                    </ReplyTo>
                  </soap:Header>
                  <soap:Body>
                    <ns2:hello xmlns:ns2="https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test">
                      <arg0>Joe</arg0>
                    </ns2:hello>
                  </soap:Body>
                </soap:Envelope>
                """
                .formatted(ID);

        given()
                .header("Content-Type", "text/xml").and().body(request)
                .when().post("/soap/addressing-anonymous")
                .then()
                .statusCode(200)
                .body(
                        Matchers.hasXPath(
                                QuarkusCxfInternalTestUtil.anyNs("Envelope", "Header", "Action") + "/text()",
                                CoreMatchers.is(
                                        HelloService.NS + "/HelloService/helloResponse")),
                        Matchers.hasXPath(
                                QuarkusCxfInternalTestUtil.anyNs("Envelope", "Header", "MessageID") + "/text()",
                                Matchers.notNullValue(String.class)),
                        Matchers.hasXPath(
                                QuarkusCxfInternalTestUtil.anyNs("Envelope", "Header", "To") + "/text()",
                                CoreMatchers.is("http://www.w3.org/2005/08/addressing/anonymous")),
                        Matchers.hasXPath(
                                QuarkusCxfInternalTestUtil.anyNs("Envelope", "Header", "RelatesTo") + "/text()",
                                CoreMatchers.is(ID)));
    }

    @Test
    void addressingSoapHeadersSent() throws Exception {

        given()
                .body("Joe")
                .post("/ws-addressing-client/call-addressing-headers-enforcer")
                .then()
                .statusCode(200)
                .body(Matchers.is("Hello Joe from AddressingAnonymousImpl"));

    }

}
