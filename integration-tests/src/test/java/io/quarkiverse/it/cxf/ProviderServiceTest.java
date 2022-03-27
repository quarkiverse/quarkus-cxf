package io.quarkiverse.it.cxf;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.not;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkiverse.it.cxf.provider.SOAPMessageProvider;
import io.quarkiverse.it.cxf.provider.SourceMessageProvider;
import io.quarkiverse.it.cxf.provider.SourcePayloadProvider;
import io.quarkiverse.it.cxf.provider.StreamSourcePayloadProvider;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ProviderServiceTest {

    private static final String SOAP_PAYLOAD_XML = "           <sayHello xmlns=\"http://provider.cxf.it.quarkiverse.io/\">\n" +
            "               <text>Hello</text>\n" +
            "           </sayHello>\n";

    private static final String SOAP_ENVELOPE_XML = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:prov=\"http://provider.cxf.it.quarkiverse.io/\">\n"
            +
            "   <soapenv:Header/>\n" +
            "   <soapenv:Body>\n" +
            "      <prov:invoke>\n" +
            SOAP_PAYLOAD_XML +
            "      </prov:invoke>\n" +
            "   </soapenv:Body>\n" +
            "</soapenv:Envelope>";

    @Inject
    SourcePayloadProvider domSourcePayloadProvider;

    @Inject
    SOAPMessageProvider soapMessageProvider;

    @Inject
    SourceMessageProvider sourceMessageProvider;

    @Inject
    StreamSourcePayloadProvider streamSourcePayloadProvider;

    @Test
    public void testInjected() {
        Assertions.assertNotNull(domSourcePayloadProvider);
        Assertions.assertNotNull(soapMessageProvider);
        Assertions.assertNotNull(sourceMessageProvider);
        Assertions.assertNotNull(streamSourcePayloadProvider);
    }

    @Test
    public void testSOAPMessageProvider() throws Exception {
        given()
                .header("Content-Type", "text/xml")
                .and()
                .body(SOAP_ENVELOPE_XML)
                .when().post("/soap/soap-message")
                .then()
                .statusCode(200)
                .body(containsString("Hello from SOAPMessageProvider"))
                .and()
                .body(containsString("http://schemas.xmlsoap.org/soap/envelope/"));
    }

    @Test
    public void testSourceMessageProvider() throws Exception {
        given()
                .header("Content-Type", "text/xml")
                .and()
                .body(SOAP_ENVELOPE_XML)
                .when().post("/soap/source-message")
                .then()
                .statusCode(200)
                .body(containsString("Hello from SourceMessageProvider"))
                .and()
                .body(containsString("http://schemas.xmlsoap.org/soap/envelope/"));
    }

    @Test
    public void testSourcePayloadProvider() throws Exception {
        given()
                .header("Content-Type", "text/xml")
                .and()
                .body(SOAP_PAYLOAD_XML)
                .when().post("/soap/source-payload")
                .then()
                .statusCode(200)
                .body(containsString("Hello from SourcePayloadProvider"))
                .and()
                .body(not(containsString("http://schemas.xmlsoap.org/soap/envelope/")));
    }

    @Test
    public void testStreamSourcePayloadProvider() throws Exception {
        given()
                .header("Content-Type", "text/xml")
                .and()
                .body(SOAP_PAYLOAD_XML)
                .when().post("/soap/stream-source-payload")
                .then()
                .statusCode(200)
                .body(containsString("Hello from StreamSourcePayloadProvider"))
                .and()
                .body(not(containsString("http://schemas.xmlsoap.org/soap/envelope/")));
    }
}
