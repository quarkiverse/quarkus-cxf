package io.quarkiverse.cxf.perf.uuid.client;

import java.io.IOException;
import java.util.UUID;

import org.eclipse.microprofile.config.ConfigProvider;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
@QuarkusTestResource(EchoUuidClientTestResource.class)
public class EchoUuidClientTest {

    @Test
    void echoUuidWsVertxSync() throws IOException {
        assertClient("echoUuidWsVertx/sync");
    }

    @Test
    void echoUuidWsVertxAsync() throws IOException {
        assertClient("echoUuidWsVertx/async");
    }

    @Test
    void echoUuidWsUrlConnectionSync() throws IOException {
        assertClient("echoUuidWsUrlConnection/sync");
    }

    private void assertClient(String client) {
        final String uuid = UUID.randomUUID().toString();
        RestAssured.given()
                .contentType("text/plain")
                .body(uuid)
                .post("/clients/" + client)
                .then()
                .statusCode(200)
                .body(Matchers.is(uuid));
    }

    @Test
    void serviceAvailable() throws IOException {

        final String serviceUri = ConfigProvider.getConfig().getValue("qcxf.uuid-service.baseUri",
                String.class) + "/echo-uuid-ws/soap-1.1";

        final String uuid = UUID.randomUUID().toString();
        /* Ensure the service works */
        RestAssured.given()
                .contentType("text/xml")
                .accept("*/*")
                .header("Connection", "Keep-Alive")
                .body(
                        String.format(
                                """
                                        <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"><soap:Body><ns2:echoUuid xmlns:ns2="http://l2x6.org/echo-uuid-ws/"><uuid>%s</uuid></ns2:echoUuid></soap:Body></soap:Envelope>
                                        """,
                                uuid))
                .post(serviceUri)
                .then()
                .statusCode(200)
                .contentType("text/xml")
                .body(
                        Matchers.is(String.format(
                                """
                                        <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"><soap:Body><ns2:echoUuidResponse xmlns:ns2="http://l2x6.org/echo-uuid-ws/"><return>%s</return></ns2:echoUuidResponse></soap:Body></soap:Envelope>
                                        """,
                                uuid).trim()));

    }
}
