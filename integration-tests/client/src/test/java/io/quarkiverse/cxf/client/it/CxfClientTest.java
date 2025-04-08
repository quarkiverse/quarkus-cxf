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
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.Random;

import org.apache.commons.io.IOUtils;
import org.apache.cxf.transport.http.URLConnectionHTTPConduit;
import org.assertj.core.api.Assertions;
import org.eclipse.microprofile.config.ConfigProvider;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkiverse.cxf.CXFClientInfo;
import io.quarkiverse.cxf.vertx.http.client.VertxHttpClientHTTPConduit;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.vertx.core.json.JsonObject;

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

        String proxyPort = ConfigProvider.getConfig().getValue("cxf.it.calculator.proxy.port", String.class);
        String uri = ConfigProvider.getConfig().getValue("cxf.it.calculator.hostNameUri", String.class);
        /* Make sure nothing was proxied before this test */
        RestAssured.given()
                .get("http://localhost:" + proxyPort)
                .then()
                .statusCode(200)
                .body("$", Matchers.hasSize(0));

        RestAssured.given()
                .queryParam("a", 4)
                .queryParam("b", 5)
                .get("/cxf/client/calculator/proxiedCalculator/multiply")
                .then()
                .statusCode(200)
                .body(is(String.valueOf(20)));

        /* Make sure the SOAP request passed the proxy server */
        RestAssured.given()
                .get("http://localhost:" + proxyPort)
                .then()
                .statusCode(200)
                .body(
                        "$", Matchers.hasSize(1),
                        "[0]", Matchers.equalTo("POST " + uri
                                + "/calculator-ws/CalculatorService <soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><ns2:multiply xmlns:ns2=\"http://www.jboss.org/eap/quickstarts/wscalculator/Calculator\"><arg0>4</arg0><arg1>5</arg1></ns2:multiply></soap:Body></soap:Envelope>"));
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
                Files.copy(in, staticCopyPath, StandardCopyOption.REPLACE_EXISTING);
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
     * Make sure that a request scoped client backed by Vert.x {@link io.vertx.core.http.HttpClient} does not leak
     * threads
     */
    @Test
    void soakRequestScopedVertxHttpClient() {
        soak("requestScopedVertxHttpClient", VertxHttpClientHTTPConduit.class.getName());
    }

    /**
     * Make sure that a request scoped client backed by {@link HttpURLConnection} does not leak threads.
     */
    @Test
    void soakRequestScopedUrlConnectionClient() {
        soak("requestScopedUrlConnectionClient", URLConnectionHTTPConduit.class.getName());
    }

    @Test
    void dynamicClientConfiguration() {

        final String calculatorBaseUri = ConfigProvider.getConfig().getValue("cxf.it.calculator.baseUri", String.class);
        RestAssured.given()
                .queryParam("a", 4)
                .queryParam("b", 3)
                .queryParam("baseUri", calculatorBaseUri)
                .get("/cxf/dynamic-client/add")
                .then()
                .statusCode(200)
                .body(is("7"));

        final String skewedCalculatorBaseUri = ConfigProvider.getConfig().getValue("cxf.it.skewed-calculator.baseUri",
                String.class);
        RestAssured.given()
                .queryParam("a", 4)
                .queryParam("b", 3)
                .queryParam("baseUri", skewedCalculatorBaseUri)
                .get("/cxf/dynamic-client/add")
                .then()
                .statusCode(200)
                .body(is("107"));

    }

    private void soak(String client, String expectedConduit) {
        RestAssured.given()
                .get("/cxf/client/clientInfo/" + client + "/httpConduit")
                .then()
                .statusCode(200)
                .body(CoreMatchers.is(expectedConduit));

        /* Make sure that the clients injected into a @RequestScoped bean are re-initialized on each request */
        final String calculatorBaseUri = ConfigProvider.getConfig().getValue("cxf.it.calculator.baseUri", String.class);
        final String skewedCalculatorBaseUri = ConfigProvider.getConfig().getValue("cxf.it.skewed-calculator.baseUri",
                String.class);
        for (int i = 0; i < 2; i++) {
            final String body = RestAssured.given()
                    .get("/cxf/client/clientInfo/" + client + "/dynamicEndpointAddress")
                    .then()
                    .statusCode(200)
                    .extract().body().asString();
            JsonObject json = new JsonObject(body);
            Assertions.assertThat(json.getString("urlBefore"))
                    .isEqualTo(calculatorBaseUri + "/calculator-ws/CalculatorService");
            Assertions.assertThat(json.getString("urlAfter"))
                    .isEqualTo(skewedCalculatorBaseUri + "/calculator-ws/CalculatorService");
            Assertions.assertThat(json.getString("result")).isEqualTo("107");
        }

        final Random rnd = new Random();
        // we divide by 2 to avoid overflow
        int a = rnd.nextInt() / 2;
        int b = rnd.nextInt() / 2;
        int expected = a + b;

        int requestCount = Integer
                .parseInt(Optional.ofNullable(System.getenv("QUARKUS_CXF_CLIENT_SOAK_ITERATIONS")).orElse("300"));
        int acceptableDeviation = Integer
                .parseInt(Optional.ofNullable(System.getenv("QUARKUS_CXF_CLIENT_SOAK_ACCEPTABLE_THREAD_COUNT_DEVIATION"))
                        .orElse("5"));

        if (requestCount < 30) {
            log.infof("QUARKUS_CXF_CLIENT_SOAK_ITERATIONS = %d is too low, using %d", requestCount, 30);
            requestCount = 30;
        }
        final int checkPoint = 10;
        int threadsAtCheckpoint = -1;
        log.infof("Performing %d interations", requestCount);
        for (int i = 0; i < requestCount; i++) {
            RestAssured.given()
                    .queryParam("a", a)
                    .queryParam("b", b)
                    .get("/cxf/client/calculator/" + client + "/add")
                    .then()
                    .statusCode(200)
                    .body(is(String.valueOf(expected)));
            final int activeThreadCount = activeThreadCount();
            log.infof("Soaked round %d, activeTreads: %d", i, activeThreadCount);

            if (i < checkPoint) {
                /* Nothing to do */
            } else if (i == checkPoint) {
                threadsAtCheckpoint = activeThreadCount;
            } else {
                if (activeThreadCount - threadsAtCheckpoint > acceptableDeviation) {
                    Assertions.fail("At iteration " + i + ", there is more active threads (" + activeThreadCount
                            + ") than threads at iteration " + checkPoint + " (" + threadsAtCheckpoint
                            + ") including acceptable deviation " + acceptableDeviation);
                }
            }
        }
    }

    private int activeThreadCount() {
        return Integer.parseInt(RestAssured.given()
                .get("/cxf/client/activeThreadCount")
                .then()
                .statusCode(200)
                .extract().body().asString());
    }

}
