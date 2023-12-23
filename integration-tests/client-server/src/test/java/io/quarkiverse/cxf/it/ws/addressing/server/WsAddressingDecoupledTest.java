package io.quarkiverse.cxf.it.ws.addressing.server;

import static io.quarkiverse.cxf.test.QuarkusCxfClientTestUtil.anyNs;
import static io.restassured.RestAssured.given;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import jakarta.xml.ws.BindingProvider;

import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.JAXWSAConstants;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkiverse.cxf.it.ws.addressing.server.decoupled.WsAddressingService;
import io.quarkiverse.cxf.test.QuarkusCxfClientTestUtil;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

@QuarkusTest
public class WsAddressingDecoupledTest {

    @Test
    void wsdl() {
        RestAssuredConfig config = RestAssured.config();
        config.getXmlConfig().namespaceAware(false);
        given()
                .config(config)
                .when().get("/soap/addressing-decoupled?wsdl")
                .then()
                .statusCode(200)
                .body(
                        Matchers.hasXPath(
                                anyNs("definitions", "binding", "UsingAddressing")
                                        + "/@*[local-name() = 'required']",
                                CoreMatchers.is("true")),
                        Matchers.hasXPath(
                                "local-name(" + anyNs("definitions", "Policy", "Addressing") + ")",
                                CoreMatchers.is("Addressing")));
    }

    @Test
    public void decoupled() throws Exception {
        final WsAddressingService client = QuarkusCxfClientTestUtil.getClient(WsAddressingService.class,
                "/soap/addressing-decoupled");

        final AddressingProperties addrProperties = new AddressingProperties();
        final EndpointReferenceType replyTo = new EndpointReferenceType();
        final AttributedURIType replyToURI = new AttributedURIType();
        final String baseURL = QuarkusCxfClientTestUtil.getServerUrl();
        replyToURI.setValue(baseURL + "/ws-addressing-target/replyTo");
        replyTo.setAddress(replyToURI);
        addrProperties.setReplyTo(replyTo);

        final String uuid = UUID.randomUUID().toString();
        final AttributedURIType messageId = new AttributedURIType();
        messageId.setValue(uuid);
        addrProperties.setMessageID(messageId);

        final Map<String, Object> requestContext = ((BindingProvider) client).getRequestContext();
        requestContext.put(JAXWSAConstants.CLIENT_ADDRESSING_PROPERTIES, addrProperties);

        Assertions.assertThat(client.echo("Foo")).isNull();

        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
            final ExtractableResponse<Response> response = given()
                    .get("/ws-addressing-target/replyTo/" + uuid)
                    .then()
                    .extract();

            return response.statusCode() == 200 && response.body().asString().contains("Foo from WsAddressingService");

        });

    }

}
