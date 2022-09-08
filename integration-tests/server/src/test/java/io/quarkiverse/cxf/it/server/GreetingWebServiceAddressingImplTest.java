package io.quarkiverse.cxf.it.server;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import javax.jws.WebService;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkiverse.cxf.test.QuarkusCxfClientTestUtil;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;

@QuarkusTest
public class GreetingWebServiceAddressingImplTest extends AbstractGreetingWebServiceTest {

    @BeforeAll
    static void setup() {
        greetingWS = QuarkusCxfClientTestUtil.getClient(GreetingWebServiceAddressingImpl.class, "/soap/greeting-addressing");
    }

    @Test
    void wsdl() {
        RestAssuredConfig config = RestAssured.config();
        config.getXmlConfig().namespaceAware(false);
        given()
                .config(config)
                .when().get(QuarkusCxfClientTestUtil.getEndpointUrl(greetingWS) + "?wsdl")
                .then()
                .statusCode(200)
                .body(
                        Matchers.hasXPath(
                                "/*[local-name() = 'definitions']/*[local-name() = 'binding']/*[local-name() = 'UsingAddressing']/@*[local-name() = 'required']",
                                CoreMatchers.is("true")),
                        Matchers.hasXPath(
                                "local-name(/*[local-name() = 'definitions']/*[local-name() = 'Policy']/*[local-name() = 'Addressing'])",
                                CoreMatchers.is("Addressing")));
    }

    @Test
    @Override
    void rawSoap() {
        // The service has @Addressing(required = true) so sending a message without addressing headers must fail
        given()
                .header("Content-Type", "text/xml").and().body(SOAP_REQUEST)
                .when().post(QuarkusCxfClientTestUtil.getEndpointUrl(greetingWS))
                .then()
                .statusCode(500)
                .body(containsString("A required header representing a Message Addressing Property is not present"));
    }

    @Test
    void rawAddressing() {
        final String ID = "urn:uuid:50784fd1-f67e-4493-b24c-5850fb38736f";
        final String request = "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\">\n"
                + "  <soap:Header>\n"
                + "    <Action xmlns=\"http://www.w3.org/2005/08/addressing\" soap:mustUnderstand=\"true\">http://server.it.cxf.quarkiverse.io/GreetingWebServiceAddressingImpl/replyRequest</Action>\n"
                + "    <MessageID xmlns=\"http://www.w3.org/2005/08/addressing\" soap:mustUnderstand=\"true\">" + ID
                + "</MessageID>\n"
                + "    <To xmlns=\"http://www.w3.org/2005/08/addressing\" soap:mustUnderstand=\"true\">http://localhost:8081/soap/greeting-addressing</To>\n"
                + "    <ReplyTo xmlns=\"http://www.w3.org/2005/08/addressing\" soap:mustUnderstand=\"true\">\n"
                + "      <Address>http://www.w3.org/2005/08/addressing/anonymous</Address>\n"
                + "    </ReplyTo>\n"
                + "  </soap:Header>\n"
                + "  <soap:Body>\n"
                + "    <ns2:reply xmlns:ns2=\"http://server.it.cxf.quarkiverse.io/\">\n"
                + "      <text>bar</text>\n"
                + "    </ns2:reply>\n"
                + "  </soap:Body>\n"
                + "</soap:Envelope>";

        given()
                .header("Content-Type", "text/xml").and().body(request)
                .when().post(QuarkusCxfClientTestUtil.getEndpointUrl(greetingWS))
                .then()
                .statusCode(200)
                .body(
                        Matchers.hasXPath(
                                "/*[local-name() = 'Envelope']/*[local-name() = 'Header']/*[local-name() = 'Action']/text()",
                                CoreMatchers.is(
                                        "http://server.it.cxf.quarkiverse.io/GreetingWebServiceAddressingImpl/replyResponse")),
                        Matchers.hasXPath(
                                "/*[local-name() = 'Envelope']/*[local-name() = 'Header']/*[local-name() = 'MessageID']/text()",
                                Matchers.notNullValue(String.class)),
                        Matchers.hasXPath(
                                "/*[local-name() = 'Envelope']/*[local-name() = 'Header']/*[local-name() = 'To']/text()",
                                CoreMatchers.is("http://www.w3.org/2005/08/addressing/anonymous")),
                        Matchers.hasXPath(
                                "/*[local-name() = 'Envelope']/*[local-name() = 'Header']/*[local-name() = 'RelatesTo']/text()",
                                CoreMatchers.is(ID)));
    }

    @Override
    protected String getServiceInterface() {
        return "GreetingWebServiceAddressingImpl";
    }

    /**
     * We need an interface for javax.xml.ws.Service.getPort(Class<T>) to be able to create a dynamic proxy.
     * Otherwise, the client is served by GreetingWebServiceAddressingImpl
     */
    @WebService(targetNamespace = "http://server.it.cxf.quarkiverse.io/", name = "GreetingWebServiceAddressingImpl")
    public interface GreetingWebServiceAddressingImpl extends GreetingWebService {
    }

}
