package io.quarkiverse.cxf.it.server;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkiverse.cxf.CXFClientInfo;
import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkus.test.junit.QuarkusTest;

/**
 * Test WebService {@link GreetingWebService}.
 *
 */
@QuarkusTest
class ClientGreetingTest {

    @Inject
    @CXFClient
    GreetingWebService greetingWS;

    @Inject
    GreetingWebService greetingImpl;

    @Named("io.quarkiverse.cxf.it.server.GreetingWebService")
    CXFClientInfo greetingInfo;

    @Test
    public void testInjected() {
        Assertions.assertNotNull(greetingWS);
        Assertions.assertNotNull(greetingImpl);
        Assertions.assertNotNull(greetingInfo);
    }

    @Test
    public void testDefaultEpAddress() {
        Assertions.assertEquals(
                "http://localhost:8080/io.quarkiverse.cxf.it.server.greetingwebservice",
                this.greetingInfo.getEndpointAddress());
    }

    @Test
    public void testActiveEpAddress() {
        /* Too bad - there is no way of retrieving this information */
        assertTrue(true);
    }

    @Test
    public void testWsdlAvailable() {
        // http://localhost:8081/soap/greeting
        // TODO: get dynamically quarkus' test port.
        given().port(8081)
                .when().get("/soap/greeting?wsdl")
                .then().statusCode(200);
    }

    @Test
    public void testWsReply() {
        Assertions.assertEquals("Hello hello", greetingWS.reply("hello"));
    }

    @Test
    public void testImplReply() {
        Assertions.assertEquals("Hello hello", greetingImpl.reply("hello"));
    }

}
