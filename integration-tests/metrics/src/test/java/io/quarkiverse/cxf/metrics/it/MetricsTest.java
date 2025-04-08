package io.quarkiverse.cxf.metrics.it;

import static io.restassured.RestAssured.given;

import java.util.Map;

import org.assertj.core.api.Assertions;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;

@QuarkusTest
public class MetricsTest {

    @Test
    void serverAndClient() {
        {
            final Map<String, Object> metrics = getMetrics();
            /* There should be no cxf metrics available before we call anything */
            Assertions.assertThat(metrics.get("cxf.server.requests")).isNull();

            // There may exist client metrics caused by Hc5Test
            // Assertions.assertThat(metrics.get("cxf.client.requests")).isNull();
        }

        /* First send a direct request to the service circumventing the in-app client */
        final String SOAP_REQUEST = "<x:Envelope xmlns:x=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:cxf=\"http://it.server.metrics.cxf.quarkiverse.io/\">\n"
                +
                "   <x:Header/>\n" +
                "   <x:Body>\n" +
                "      <cxf:hello>\n" +
                "          <text>foo</text>\n" +
                "      </cxf:hello>\n" +
                "   </x:Body>\n" +
                "</x:Envelope>";
        given()
                .header("Content-Type", "text/xml")
                .body(SOAP_REQUEST)
                .when()
                .post("/soap/hello")
                .then()
                .statusCode(200)
                .body(CoreMatchers.containsString("Hello foo"));

        {
            final Map<String, Object> metrics = getMetrics();
            @SuppressWarnings("unchecked")
            Map<String, Object> serverRequests = (Map<String, Object>) metrics.get("cxf.server.requests");
            Assertions.assertThat(serverRequests).isNotNull();
            Assertions.assertThat(serverRequests.get(
                    "count;exception=None;faultCode=None;method=POST;operation=hello;outcome=SUCCESS;status=200;uri=/soap/hello"))
                    .isEqualTo(1);
            Assertions.assertThat((Float) serverRequests.get(
                    "elapsedTime;exception=None;faultCode=None;method=POST;operation=hello;outcome=SUCCESS;status=200;uri=/soap/hello"))
                    .isGreaterThan(0.0F);
        }

        final Config config = ConfigProvider.getConfig();
        final int port = config.getValue("quarkus.http.test-port", Integer.class);

        /* Now send a request using the in-app client */
        given()
                .body("Joe")
                .when()
                .post("/metrics/client/hello")
                .then()
                .statusCode(200)
                .body(CoreMatchers.containsString("Hello Joe"));
        {
            final Map<String, Object> metrics = getMetrics();
            @SuppressWarnings("unchecked")
            Map<String, Object> serverRequests = (Map<String, Object>) metrics.get("cxf.server.requests");
            Assertions.assertThat(serverRequests).isNotNull();
            Assertions.assertThat(serverRequests.get(
                    "count;exception=None;faultCode=None;method=POST;operation=hello;outcome=SUCCESS;status=200;uri=/soap/hello"))
                    .isEqualTo(2);
            Assertions.assertThat((Float) serverRequests.get(
                    "elapsedTime;exception=None;faultCode=None;method=POST;operation=hello;outcome=SUCCESS;status=200;uri=/soap/hello"))
                    .isGreaterThan(0.0F);

            Map<String, Object> clientRequests = (Map<String, Object>) metrics.get("cxf.client.requests");
            Assertions.assertThat(clientRequests).isNotNull();
            Assertions.assertThat(clientRequests.get(
                    "count;exception=None;faultCode=None;method=POST;operation=hello;outcome=SUCCESS;status=200;uri=http://localhost:"
                            + port + "/soap/hello"))
                    .isEqualTo(1);
            Assertions.assertThat((Float) clientRequests.get(
                    "elapsedTime;exception=None;faultCode=None;method=POST;operation=hello;outcome=SUCCESS;status=200;uri=http://localhost:"
                            + port + "/soap/hello"))
                    .isGreaterThan(0.0F);

        }

    }

    public static Map<String, Object> getMetrics() {
        final String body = RestAssured.given()
                .header("Content-Type", "application/json")
                .get("/q/metrics/json")
                .then()
                .statusCode(200)
                .extract().body().asString();
        final JsonPath jp = new JsonPath(body);
        return jp.getJsonObject("$");
    }

}
