package io.quarkiverse.cxf.it.server;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class GreetingSOAPHandlerTest {

    /**
     * Make sure that the handler gets installed properly
     */
    @Test
    void soapHandler() {
        String xml = "<x:Envelope xmlns:x=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:cxf=\"http://server.it.cxf.quarkiverse.io/\">\n"
                +
                "   <x:Header/>\n" +
                "   <x:Body>\n" +
                "      <cxf:reply>\n" +
                "          <text>handler</text>\n" +
                "      </cxf:reply>\n" +
                "   </x:Body>\n" +
                "</x:Envelope>";

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
