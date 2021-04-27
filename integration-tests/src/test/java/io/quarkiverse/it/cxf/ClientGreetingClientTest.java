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
 * {@link GreetingClientWebService} is SEI-identical with {@link GreetingWebService}. This test is
 * about whether we can access {@code GreetingWebService}'s EP via {@code GreetingClientWebService}.
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
    public void testClientProxyInjected() {
        Assertions.assertNotNull(defaultClient);
        Assertions.assertNotNull(greetingClient);
        Assertions.assertNotNull(faultyClient);
    }

    @Test
    public void testClientProxyIsproxy() {
        Assertions.assertTrue(Proxy.isProxyClass(defaultClient.getClass()));
        Assertions.assertTrue(Proxy.isProxyClass(greetingClient.getClass()));
        Assertions.assertTrue(Proxy.isProxyClass(faultyClient.getClass()));
    }

    @Test
    public void testClientInfoInjected() {
        Assertions.assertNotNull(greetingInfo);
    }

    @Test
    public void testDefaultEpAddress() {
        Assertions.assertEquals(
                "http://localhost:8080/io.quarkiverse.it.cxf.greetingclientwebservice",
                this.greetingInfo.getEndpointAddress());
    }

    @Test
    public void testActiveEpAddress() {
        /* Too bad - there is no way of retrieving this information */
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
    public void testPing() {
        Assertions.assertEquals("Hello hello", greetingClient.ping("hello"));
        Assertions.assertEquals("Hello hello", defaultClient.ping("hello"));
    }

    @Test
    public void testOutInterceptorPresent() {
        Assertions.assertThrows(SOAPFaultException.class, () -> {
            faultyClient.ping("hello");
        });
    }
}
