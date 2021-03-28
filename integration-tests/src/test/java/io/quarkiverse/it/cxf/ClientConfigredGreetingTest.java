package io.quarkiverse.it.cxf;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkus.test.junit.QuarkusTest;

/**
 * Test configured clients of internal WebService GreetingWebService.
 *
 */
@QuarkusTest
class ClientConfigredGreetingTest {
    // TODO:
    // Problem: This test should also work with GreetingWebService. It does not
    // cause a GreetingWebService proxy client is not produced by Quarkus-CXF (
    // CxfClientProducer). How come?

    @Inject
    @CXFClient
    GreetingClientWebService greetingWS;

    //    @Inject
    //    @CXFClient(config = "featured-foo")
    //    GreetingClientWebService featuredWS;
    //
    //    @Named("io.quarkiverse.it.cxf.GreetingWebService")
    //    CXFClientInfo greetingInfo;
    //
    @Test
    public void test_clients_injected() {
        Assertions.assertNotNull(greetingWS);
    }
    //
    //    @Test
    //    public void test_infos_injected() {
    //        Assertions.assertNotNull(greetingInfo);
    //    }
    //
    //    @Test
    //    public void test_default_ep_address() {
    //        Assertions.assertEquals(
    //                "http://localhost:8080/io.quarkiverse.it.cxf.greetingwebservice",
    //                this.greetingInfo.getEndpointAddress());
    //    }
    //
    //    @Test
    //    public void test_active_ep_address() {
    //        /* Too bad - there is no way of retrieving this information */
    //    }
    //
    //    @Test
    //    public void test_wsdl_available() {
    //        // http://localhost:8081/soap/greeting
    //        // TODO: get dynamically quarkus' test port.
    //        given().port(8081)
    //                .when().get("/soap/greeting?wsdl")
    //                .then().statusCode(200);
    //    }
    //
    //    @Test
    //    public void test_ping() {
    //        Assertions.assertEquals("Hello hello", greetingWS.ping("hello"));
    //    }
    //
    //    @Test
    //    public void test_featured_ping() {
    //        Assertions.assertThrows(SOAPFaultException.class, () -> {
    //            featuredWS.ping("hello");
    //        });
    //    }
}
