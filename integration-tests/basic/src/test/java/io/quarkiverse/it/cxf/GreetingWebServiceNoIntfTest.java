package io.quarkiverse.it.cxf;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import java.lang.reflect.Proxy;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class GreetingWebServiceNoIntfTest {

    @Inject
    public GreetingWebServiceNoIntf greetingWebServiceNoIntf;

    @Test
    void testIsNotProxy() {
        Assertions.assertFalse(Proxy.isProxyClass(greetingWebServiceNoIntf.getClass()));
    }

    @Test
    void testCxfClient() {
        Assertions.assertEquals("Hello bar", greetingWebServiceNoIntf.reply("bar"));
    }

    @Test
    void testPing() {
        String ret = null;
        try {
            ret = greetingWebServiceNoIntf.ping("bar");
        } catch (GreetingException e) {
        }
        Assertions.assertEquals("Hello bar", ret);
    }

    @Test
    void testSoapEndpoint() {
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
                .when().post("/soap/greeting-no-intf")
                .then()
                .statusCode(200)
                .body(containsString("Hello foo"));
    }

    @Test
    void testSoap12Binding() {
        given()
                .when().get("/soap/greeting-no-intf?wsdl")
                .then()
                .statusCode(200)
                .body(containsString("http://schemas.xmlsoap.org/wsdl/soap12/"));
    }

}
