package io.quarkiverse.it.cxf;

import static io.restassured.RestAssured.given;

import java.lang.reflect.Proxy;

import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.ws.soap.SOAPFaultException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkiverse.cxf.CXFClientInfo;
import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkus.test.junit.QuarkusTest;

/**
 * GreetingClientWebService is SEI-identical with GreetingWebService. Here we test whether we can access
 * GreetingWebService's EP via GreetingClientWebService.
 */
@QuarkusTest
class ClientGreetingClientTest {

    @Inject
    @CXFClient
    GreetingClientWebService defaultClient;

    @Inject
    @CXFClient("greetingclient")
    GreetingClientWebService greetingClient;

    @Inject
    @CXFClient("greetingclient-fault")
    GreetingClientWebService faultyClient;

    @Inject
    @Named("io.quarkiverse.it.cxf.GreetingClientWebService")
    CXFClientInfo greetingInfo;

    @Test
    public void test_clientproxy_injected() {
        Assertions.assertNotNull(defaultClient);
        Assertions.assertNotNull(greetingClient);
        Assertions.assertNotNull(faultyClient);
    }

    @Test
    public void test_clientproxy_isproxy() {
        Assertions.assertTrue(Proxy.isProxyClass(defaultClient.getClass()));
        Assertions.assertTrue(Proxy.isProxyClass(greetingClient.getClass()));
        Assertions.assertTrue(Proxy.isProxyClass(faultyClient.getClass()));
    }

    @Test
    public void test_clientinfo_injected() {
        Assertions.assertNotNull(greetingInfo);
    }

    @Test
    public void test_default_ep_address() {
        Assertions.assertEquals(
                "http://localhost:8080/io.quarkiverse.it.cxf.greetingclientwebservice",
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
        Assertions.assertEquals("Hello hello", greetingClient.ping("hello"));
        Assertions.assertEquals("Hello hello", defaultClient.ping("hello"));
    }

    @Test
    public void test_out_interceptor_present() {
        Assertions.assertThrows(SOAPFaultException.class, () -> {
            faultyClient.ping("hello");
        });
    }
}
