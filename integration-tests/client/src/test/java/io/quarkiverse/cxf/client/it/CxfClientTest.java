package io.quarkiverse.cxf.client.it;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Random;

import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
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

    private static final Logger log = Logger.getLogger(CxfClientTest.class);

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
     * Test whether a client with proxy and proxy auth set works properly
     *
     * @param clientKey
     */
    @Test
    void multiplyProxy() {
        RestAssured.given()
                .queryParam("a", 4)
                .queryParam("b", 5)
                .get("/cxf/client/calculator/proxiedCalculator/multiply")
                .then()
                .statusCode(200)
                .body(is(String.valueOf(20)));
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
     * Test whether passing multiple parameters to the client works properly
     *
     * @param clientKey
     */
    @ParameterizedTest
    @ValueSource(strings = { "default", "myCalculator", "mySkewedCalculator" })
    void addNumberAndOperands(String clientKey) {
        final int expected = "mySkewedCalculator".equals(clientKey) ? 112 : 12;
        RestAssured.given()
                .queryParam("a", 3)
                .queryParam("b", 4)
                .queryParam("c", 5)
                .get("/cxf/client/calculator/" + clientKey + "/addNumberAndOperands")
                .then()
                .statusCode(200)
                .body(is(String.valueOf(expected)));
    }

    /**
     * Test whether passing multiple parameters to the client works properly
     *
     * @param clientKey
     */
    @ParameterizedTest
    @ValueSource(strings = { "default", "myCalculator", "mySkewedCalculator" })
    void addArray(String clientKey) {
        final int expected = "mySkewedCalculator".equals(clientKey) ? 112 : 12;
        RestAssured.given()
                .queryParam("a", 3)
                .queryParam("b", 4)
                .queryParam("c", 5)
                .get("/cxf/client/calculator/" + clientKey + "/addArray")
                .then()
                .statusCode(200)
                .body(is(String.valueOf(expected)));
    }

    /**
     * Test whether passing multiple parameters to the client works properly
     *
     * @param clientKey
     */
    @ParameterizedTest
    @ValueSource(strings = { "default", "myCalculator", "mySkewedCalculator" })
    void addList(String clientKey) {
        final int expected = "mySkewedCalculator".equals(clientKey) ? 112 : 12;
        RestAssured.given()
                .queryParam("a", 3)
                .queryParam("b", 4)
                .queryParam("c", 5)
                .get("/cxf/client/calculator/" + clientKey + "/addList")
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

    @Test
    void basicAuth() {
        RestAssured.given()
                .queryParam("a", 7)
                .queryParam("b", 4)
                .get("/cxf/client/auth/basic/myBasicAuthCalculator/securedAdd")
                .then()
                .statusCode(200)
                .body(is("11"));
    }

    @Test
    void basicAuthAnonymous() {
        RestAssured.given()
                .queryParam("a", 7)
                .queryParam("b", 4)
                .get("/cxf/client/auth/basic/myBasicAuthAnonymousCalculator/securedAdd")
                .then()
                .statusCode(500)
                .body(containsString("HTTP response '401: "));
    }

    @Test
    void bareEcho() {
        RestAssured.given()
                .queryParam("a", 7)
                .get("/cxf/client/bare/echo")
                .then()
                .statusCode(200)
                .body(is("7"));
    }

    @Test
    void bareOperands() {
        RestAssured.given()
                .queryParam("a", 7)
                .queryParam("b", 4)
                .get("/cxf/client/bare/addOperands")
                .then()
                .statusCode(200)
                .body(is("11"));
    }

    @Test
    void bareArray() {
        RestAssured.given()
                .queryParam("a", 7)
                .queryParam("b", 4)
                .queryParam("c", 2)
                .get("/cxf/client/bare/addArray")
                .then()
                .statusCode(200)
                .body(is("13"));
    }

    /**
     * Make sure that our static copies are the same as the WSDL served by the container
     *
     * @throws IOException
     */
    @Test
    void wsdlUpToDate() throws IOException {
        wsdlUpToDate("myCalculator", "CalculatorService");
        wsdlUpToDate("myBasicAuthCalculator", "BasicAuthCalculatorService");
    }

    static void wsdlUpToDate(String clientKey, String serviceName) throws IOException {
        final String wsdlUrl = ConfigProvider.getConfig()
                .getValue("quarkus.cxf.client." + clientKey + ".wsdl", String.class);

        Path staticCopyPath = Paths.get("src/main/resources/wsdl/" + serviceName + ".wsdl");
        if (!Files.isRegularFile(staticCopyPath)) {
            /*
             * This test can be run from the test jar on Quarkus Platform
             * In that case target/classes does not exist an we have to copy
             * what's needed manually
             */
            staticCopyPath = Paths.get("target/classes/wsdl/" + serviceName + ".wsdl");
            Files.createDirectories(staticCopyPath.getParent());
            try (InputStream in = CxfClientTest.class.getClassLoader().getResourceAsStream("wsdl/" + serviceName + ".wsdl")) {
                Files.copy(in, staticCopyPath);
            }
        }
        /* The changing Docker IP address in the WSDL should not matter */
        final String sanitizerRegex = "<soap:address location=\"http://[^/]*/calculator-ws/" + serviceName
                + "\"></soap:address>";
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

    @Test
    void createEscapeHandler() {
        RestAssured.given()
                .body("Tom & Jerry")
                .post("/cxf/client/createEscapeHandler/MinimumEscapeHandler")
                .then()
                .statusCode(200)
                .body(is("Tom &amp; Jerry"));

        RestAssured.given()
                .body("Tom & Jerry")
                .post("/cxf/client/createEscapeHandler/NoEscapeHandler")
                .then()
                .statusCode(200)
                .body(is("Tom & Jerry"));
    }

    /**
     * Make sure that a request scoped client backed by {@link HttpClient} does not leak threads
     * - see <a href=
     * "https://github.com/quarkiverse/quarkus-cxf/issues/992">https://github.com/quarkiverse/quarkus-cxf/issues/992</a>.
     */
    @Test
    void soakRequestScopedHttpClient() {
        soak("requestScopedHttpClient");

    }

    /**
     * Make sure that a request scoped client backed by {@link HttpURLConnection} does not leak threads.
     */
    @Test
    void soakRequestScopedUrlConnectionClient() {
        soak("requestScopedUrlConnectionClient");

    }

    private void soak(String client) {

        final Random rnd = new Random();
        // we divide by 2 to avoid overflow
        int a = rnd.nextInt() / 2;
        int b = rnd.nextInt() / 2;
        int expected = a + b;

        final int requestCount = Integer
                .parseInt(Optional.ofNullable(System.getenv("QUARKUS_CXF_CLIENT_SOAK_ITERATIONS")).orElse("300"));
        log.infof("Performing %d interations", requestCount);
        for (int i = 0; i < requestCount; i++) {
            log.infof("Soaking round %d", i);
            RestAssured.given()
                    .queryParam("a", a)
                    .queryParam("b", b)
                    .get("/cxf/client/calculator/" + client + "/add")
                    .then()
                    .statusCode(200)
                    .body(is(String.valueOf(expected)));
        }
    }

}
