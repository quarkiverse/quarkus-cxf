package io.quarkiverse.it.cxf;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class GreetingSOAPHandlerTest {

    @Test
    void testSoapEndpoint() {
        String xml = "<x:Envelope xmlns:x=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:cxf=\"http://cxf.it.quarkiverse.io/\">\n"
                +
                "   <x:Header/>\n" +
                "   <x:Body>\n" +
                "      <cxf:reply>\n" +
                "          <text>handler</text>\n" +
                "      </cxf:reply>\n" +
                "   </x:Body>\n" +
                "</x:Envelope>";
        String cnt = "";

        given()
                .header("Content-Type", "text/xml")
                .and()
                .body(xml)
                .when().post("/soap/greeting-soap-handler")
                .then()
                .statusCode(200)
                .body(containsString("Hello handler"))
                .header("TEST-HEADER-KEY", "From SOAP Handler");
    }

}
