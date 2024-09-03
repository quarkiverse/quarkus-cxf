package io.quarkiverse.cxf.hc5.it;

import static org.hamcrest.CoreMatchers.is;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkiverse.cxf.hc5.it.HeaderToMetricsTagRequestFilter.RequestScopedHeader;
import io.quarkiverse.cxf.hc5.it.MultiplyingAddInterceptor.RequestScopedFactorHeader;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;

@QuarkusTest
@QuarkusTestResource(Hc5TestResource.class)
class Hc5Test {

    @ParameterizedTest
    @ValueSource(strings = { "sync", "async" })
    void add(String syncMode) {
        RestAssured.given()
                .header(RequestScopedHeader.header, syncMode + "-header-value")
                .queryParam("a", 7)
                .queryParam("b", 4)
                .get("/hc5/add-" + syncMode)
                .then()
                .statusCode(200)
                .body(is("11"));

    }

    @ParameterizedTest
    @ValueSource(strings = { "sync-observable", "async-observable" })
    void addObservable(String syncMode) {
        RestAssured.given()
                .header(RequestScopedHeader.header, syncMode + "-header-value")
                .queryParam("a", 7)
                .queryParam("b", 4)
                .get("/hc5/add-" + syncMode)
                .then()
                .statusCode(200)
                .body(is("11"));

        /* Make sure that the tagging done in MeterFilterProducer actually works */

        final Config config = ConfigProvider.getConfig();
        final String baseUri = config.getValue("cxf.it.calculator.baseUri", String.class);
        final Map<String, Object> metrics = getMetrics();

        @SuppressWarnings("unchecked")
        Map<String, Object> clientRequests = (Map<String, Object>) metrics.get("cxf.client.requests");
        Assertions.assertThat(clientRequests).isNotNull();
        String key = "count;exception=None;faultCode=None;method=POST;my-header=" + syncMode
                + "-header-value;operation=add;outcome=SUCCESS;status=200;uri="
                + baseUri + "/calculator-ws/CalculatorService";
        Assertions.assertThat((Integer) clientRequests.get(key)).isGreaterThan(0);
    }

    @ParameterizedTest
    @ValueSource(strings = { "sync-contextPropagation", "async-contextPropagation" })
    void addContextPropagation(String syncMode) {
        RestAssured.given()
                .queryParam("a", 7)
                .queryParam("b", 4)
                .get("/hc5/add-" + syncMode)
                .then()
                .statusCode(200)
                .body(is("0")); // (7+4) * 0 because RequestScopedFactorHeader.header has no value

        RestAssured.given()
                .header(RequestScopedFactorHeader.header, "2")
                .queryParam("a", 7)
                .queryParam("b", 4)
                .get("/hc5/add-" + syncMode)
                .then()
                .statusCode(200)
                .body(is("22")); // (7+4) * 2
    }

    /**
     * Make sure that our static copy is the same as the WSDL served by the container
     *
     * @throws IOException
     */
    @Test
    void wsdlUpToDate() throws IOException {
        final String wsdlUrl = ConfigProvider.getConfig()
                .getValue("quarkus.cxf.client.myCalculator.wsdl", String.class);

        Path staticCopyPath = Paths.get("src/main/resources/wsdl/CalculatorService.wsdl");
        if (!Files.isRegularFile(staticCopyPath)) {
            /*
             * This test can be run from the test jar on Quarkus Platform
             * In that case target/classes does not exist an we have to copy
             * what's needed manually
             */
            staticCopyPath = Paths.get("target/classes/wsdl/CalculatorService.wsdl");
            Files.createDirectories(staticCopyPath.getParent());
            try (InputStream in = Hc5Test.class.getClassLoader().getResourceAsStream("wsdl/CalculatorService.wsdl")) {
                Files.copy(in, staticCopyPath, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        /* The changing Docker IP address in the WSDL should not matter */
        final String sanitizerRegex = "<soap:address location=\"http://[^/]*/calculator-ws/CalculatorService\"></soap:address>";
        final String staticCopyContent = Files
                .readString(staticCopyPath, StandardCharsets.UTF_8)
                .replaceAll(sanitizerRegex, "");

        final String expected = RestAssured.given()
                .get(wsdlUrl)
                .then()
                .statusCode(200)
                .extract().body().asString();

        if (!expected.replaceAll(sanitizerRegex, "").equals(staticCopyContent)) {
            Files.writeString(staticCopyPath, expected, StandardCharsets.UTF_8);
            Assertions.fail("The static WSDL copy in " + staticCopyPath
                    + " went out of sync with the WSDL served by the container. The content was updated by the test, you just need to review and commit the changes.");
        }

    }

    private Map<String, Object> getMetrics() {
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
