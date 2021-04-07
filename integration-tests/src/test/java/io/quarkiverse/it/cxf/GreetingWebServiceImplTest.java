package io.quarkiverse.it.cxf;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import java.lang.reflect.Proxy;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkiverse.cxf.annotation.CXFImpl;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class GreetingWebServiceImplTest {

    @Inject
    @CXFImpl
    public GreetingWebService greetingImpl;

    @Test
    void test_is_injected() {
        Assertions.assertNotNull(greetingImpl);
    }

    @Test
    void test_is_helloresource_injected() {
        Assertions.assertNotNull(greetingImpl);
        Assertions.assertTrue(greetingImpl instanceof GreetingWebServiceImpl);
        Assertions.assertNotNull(GreetingWebServiceImpl.class.cast(greetingImpl).helloResource);
    }

    @Test
    void test_is_not_proxy() {
        Assertions.assertFalse(Proxy.isProxyClass(greetingImpl.getClass()));
    }

    @Test
    void testCxfClient() {
        Assertions.assertEquals("Hello bar", greetingImpl.reply("bar"));
    }

    @Test
    void testPing() {
        Assertions.assertEquals("Hello bar", greetingImpl.ping("bar"));
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
        String cnt = "";

        given()
                .header("Content-Type", "text/xml").and().body(xml)
                .when().post("/soap/greeting")
                .then()
                .statusCode(200)
                .body(containsString("Hello foo"));
    }

    @Test
    void testSoap12Binding() {
        given()
                .when().get("/soap/greeting?wsdl")
                .then()
                .statusCode(200)
                .body(containsString("http://schemas.xmlsoap.org/wsdl/soap12/"));
    }

    @Test
    void testRestCxfClient() {
        given()
                .when().get("/rest")
                .then()
                .statusCode(200)
                .body(containsString("Hello foo"));
    }
}
