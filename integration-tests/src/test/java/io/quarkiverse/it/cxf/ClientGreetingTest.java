package io.quarkiverse.it.cxf;

import static io.restassured.RestAssured.given;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkiverse.cxf.CXFClientInfo;
import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkus.test.junit.QuarkusTest;

/**
 * Test clients of internal WebService GreetingWebService.
 *
 */
@QuarkusTest
class ClientGreetingTest {

    @Inject
    @CXFClient
    GreetingWebService greetingWS;

    @Named("io.quarkiverse.it.cxf.GreetingWebService")
    CXFClientInfo greetingInfo;

    @Test
    public void test_clients_injected() {
        Assertions.assertNotNull(greetingWS);
    }

    @Test
    public void test_infos_injected() {
        Assertions.assertNotNull(greetingInfo);
    }

    @Test
    public void test_default_ep_address() {
        Assertions.assertEquals(
                "http://localhost:8080/io.quarkiverse.it.cxf.greetingwebservice",
                this.greetingInfo.getEndpointAddress());
    }

    @Test
    public void test_active_ep_address() {
        /* Too bad - there is no way of retrieving this information */
    }

    @Test
    public void test_wsdl_available() {
        // http://localhost:8081/soap/greeting
        // TODO: get dynamically quarkus' test port.
        given().port(8081)
                .when().get("/soap/greeting?wsdl")
                .then().statusCode(200);
    }

    @Test
    public void test_ping() {
        Assertions.assertEquals("Hello hello", greetingWS.ping("hello"));
    }
}
