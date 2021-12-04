package io.quarkiverse.it.cxf;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToXml;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import io.quarkiverse.it.cxf.GreetingClientWebService;
import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkiverse.it.cxf.wiremock.InjectWireMock;
import io.quarkiverse.it.cxf.wiremock.WireMockResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(WireMockResource.class) // configures wiremockserver as quarkus test resource
public class ClientGreetingWireMockTest {

    public static final String PING_REQUEST = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><ns2:Ping xmlns:ns2=\"http://cxf.it.quarkiverse.io/\"><text>hello</text></ns2:Ping></soap:Body></soap:Envelope>";
    public static final String PING_RESPONE = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><ns2:Ping xmlns:ns2=\"http://cxf.it.quarkiverse.io/\"><text>Hello hello</text></ns2:Ping></soap:Body></soap:Envelope>";

    @InjectWireMock // injects wiremock server which is configured by WireMockResource
    WireMockServer wireMockServer;

    @Inject
    @CXFClient
    GreetingClientWebService defaultClient;

    @BeforeEach
    public void resetWireMockServer() {
        WireMock.resetAllRequests();
    }

    @Test
    public void testPing() {
        wireMockServer.stubFor(
                post(urlEqualTo("/soap/greeting"))
                        .withRequestBody(equalToXml(PING_REQUEST))
                        .willReturn(aResponse().withBody(PING_RESPONE)));

        Assertions.assertEquals("Hello hello", defaultClient.ping("hello"));
    }

}
