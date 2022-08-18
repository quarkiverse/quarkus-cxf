package io.quarkiverse.cxf.it.server;

import javax.jws.WebService;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class GreetingWebServiceNoIntfTest extends AbstractGreetingWebServiceTest {

    @BeforeAll
    static void setup() {
        greetingWS = ClientTestUtil.getClient(GreetingWebServiceNoIntf.class, "/soap/greeting-no-intf");
    }

    @Test
    void endpointUrl() {
        Assertions.assertThat(ClientTestUtil.getEndpointUrl(greetingWS)).endsWith("/soap/greeting-no-intf");
    }

    @Override
    protected String getServiceInterface() {
        return "GreetingWebServiceNoIntf";
    }

    /**
     * We need an interface for javax.xml.ws.Service.getPort(Class<T>) to be able to create a dynamic proxy.
     * Otherwise, the client is served by GreetingWebServiceNoIntf
     */
    @WebService(targetNamespace = "http://server.it.cxf.quarkiverse.io/", name = "GreetingWebServiceNoIntf")
    public interface GreetingWebServiceNoIntf extends GreetingWebService {
    }
}
