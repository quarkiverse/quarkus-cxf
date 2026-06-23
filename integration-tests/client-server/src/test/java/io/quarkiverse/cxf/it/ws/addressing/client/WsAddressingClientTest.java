package io.quarkiverse.cxf.it.ws.addressing.client;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkiverse.cxf.test.internal.QuarkusCxfInternalTestUtil;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;

@QuarkusTest
public class WsAddressingClientTest {
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
                .header("Content-Type", "text/xml")
                .body(SOAP_REQUEST)
                .when().post("/soap/addressing-anonymous")
                .then()
                .statusCode(500)
                .body(containsString("A required header representing a Message Addressing Property is not present"));
    }

}
