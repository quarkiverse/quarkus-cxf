package io.quarkiverse.it.cxf;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.NativeImageTest;

@NativeImageTest
class GreetingWebServiceImplTestIT {
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
