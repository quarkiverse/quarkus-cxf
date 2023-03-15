package io.quarkiverse.cxf.client.it;

import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkiverse.cxf.CXFClientInfo;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
@QuarkusTestResource(CxfClientTestResource.class)
public class CxfClientTest {

    @Test
    void add() {
        RestAssured.given()
                .queryParam("a", 7)
                .queryParam("b", 4)
                .get("/cxf/calculator-client/add")
                .then()
                .statusCode(200)
                .body(is("11"));
    }

    /**
     * Test whether all ways of injecting a client work properly
     *
     * @param clientKey
     */
    @ParameterizedTest
    @ValueSource(strings = { "default", "myCalculator", "mySkewedCalculator" })
    void multiply(String clientKey) {
        final int expected = "mySkewedCalculator".equals(clientKey) ? 120 : 20;
        RestAssured.given()
                .queryParam("a", 4)
                .queryParam("b", 5)
                .get("/cxf/client/calculator/" + clientKey + "/multiply")
                .then()
                .statusCode(200)
                .body(is(String.valueOf(expected)));
    }

    /**
     * Test whether passing a complex object to the client and receiving a complex object from the client works properly
     *
     * @param clientKey
     */
    @ParameterizedTest
    @ValueSource(strings = { "default", "myCalculator", "mySkewedCalculator" })
    void addOperands(String clientKey) {
        final int expected = "mySkewedCalculator".equals(clientKey) ? 107 : 7;
        RestAssured.given()
                .queryParam("a", 3)
                .queryParam("b", 4)
                .get("/cxf/client/calculator/" + clientKey + "/addOperands")
                .then()
                .statusCode(200)
                .body(is(String.valueOf(expected)));
    }

    /**
     * Test whether the interceptor gets installed properly.
     */
    @Test
    void outInterceptor() {
        RestAssured.given()
                .queryParam("a", 3)
                .queryParam("b", 4)
                .get("/cxf/client/calculator/myFaultyCalculator/multiply")
                .then()
                .statusCode(500)
                .body(is("No luck at this time, Luke!"));
    }

    /**
     * Test whether a code-first client (without WSDL) works properly.
     */
    @Test
    void codeFirstClient() {
        RestAssured.given()
                .queryParam("a", 3)
                .queryParam("b", 4)
                .get("/cxf/client/codeFirstClient/multiply")
                .then()
                .statusCode(200)
                .body(is("12"));
    }

    /**
     * Check whether we can get the WSDL URL configured in application.properties from the application code via
     * {@link CXFClientInfo}.
     */
    @Test
    @Disabled("https://github.com/quarkiverse/quarkus-cxf/issues/491")
    void wsdlUrl() {

        final String wsdlUrl = ConfigProvider.getConfig()
                .getValue("quarkus.cxf.client.myCalculator.wsdl", String.class);
        Assertions.assertThat(wsdlUrl).endsWith("/calculator-ws/CalculatorService?wsdl");

        RestAssured.given()
                .get("/cxf/client/clientInfo/myCalculator/wsdlUrl")
                .then()
                .statusCode(200)
                .body(is(wsdlUrl));
    }

    /**
     * Check whether we can get the client endpoint URL configured in application.properties from the application code
     * via
     * {@link CXFClientInfo}.
     */
    @Test
    @Disabled("https://github.com/quarkiverse/quarkus-cxf/issues/491")
    void endpointAddress() {

        final String endpointAddress = ConfigProvider.getConfig()
                .getValue("quarkus.cxf.client.myCalculator.client-endpoint-url", String.class);
        Assertions.assertThat(endpointAddress).endsWith("/calculator-ws/CalculatorService");

        RestAssured.given()
                .get("/cxf/client/clientInfo/myCalculator/endpointAddress")
                .then()
                .statusCode(200)
                .body(is(endpointAddress));
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

        final String staticCopyPath = "src/main/resources/wsdl/CalculatorService.wsdl";
        /* The changing Docker IP address in the WSDL should not matter */
        final String sanitizerRegex = "<soap:address location=\"http://[^/]*/calculator-ws/CalculatorService\"></soap:address>";
        final String staticCopyContent = Files
                .readString(Paths.get(staticCopyPath), StandardCharsets.UTF_8)
                .replaceAll(sanitizerRegex, "");

        final String expected = RestAssured.given()
                .get(wsdlUrl)
                .then()
                .statusCode(200)
                .extract().body().asString();

        if (!expected.replaceAll(sanitizerRegex, "").equals(staticCopyContent)) {
            Files.writeString(Paths.get(staticCopyPath), expected, StandardCharsets.UTF_8);
            Assertions.fail("The static WSDL copy in " + staticCopyPath
                    + " went out of sync with the WSDL served by the container. The content was updated by the test, you just need to review and commit the changes.");
        }

    }

    @Test
    void wsdlIncluded() throws IOException {
        final String wsdl = IOUtils.resourceToString("wsdl/CalculatorService.wsdl", StandardCharsets.UTF_8,
                getClass().getClassLoader());
        /* make sure that the WSDL was included in the native image */
        RestAssured.given()
                .get("/cxf/client/resource/wsdl/CalculatorService.wsdl")
                .then()
                .statusCode(200)
                .body(is(wsdl));

    }

    /**
     * Test whether a code-first client (without WSDL) works properly.
     */
    @Test
    void clientWithRuntimeInitializedPayload() {
        RestAssured.given()
                .queryParam("a", 7)
                .queryParam("b", 8)
                .get("/cxf/client/clientWithRuntimeInitializedPayload/addOperands")
                .then()
                .statusCode(200)
                .body(is("15"));
    }

}
