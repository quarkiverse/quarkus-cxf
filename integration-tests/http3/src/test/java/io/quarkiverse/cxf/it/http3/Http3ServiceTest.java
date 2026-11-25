package io.quarkiverse.cxf.it.http3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Alias;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientAgent;
import io.vertx.core.http.HttpClientConfig;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.net.PfxOptions;

/**
 * A test of {@link ProtocolEchoImpl}, a SOAP service exposed by Quarkus CXF, called over HTTP/3.
 * <p>
 * The service is called either by SOAP clients having {@code 3} in
 * {@code quarkus.cxf.client."client-name".vertx.http-versions} or by a plain Vert.x {@link HttpClientAgent}, so that
 * there is at least some coverage that does not depend on the client side of Quarkus CXF.
 *
 * @since 4.0.0
 */
@Certificates(baseDir = "target/classes", //
        certificates = @Certificate( //
                name = "localhost", //
                password = "secret", //
                aliases = @Alias(//
                        name = "client", //
                        password = "secret", //
                        client = true), //
                formats = { Format.PKCS12 }))
@QuarkusTest
public class Http3ServiceTest {

    private static final String SERVICE_PATH = "/services/echo";
    private static final String SOAP_REQUEST = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body>"
            + "<ns2:echo xmlns:ns2=\"urn:protocol-echo\"><body>x</body></ns2:echo>"
            + "</soap:Body></soap:Envelope>";

    private static Vertx vertx;

    @BeforeAll
    static void beforeAll() {
        vertx = Vertx.vertx();
    }

    @AfterAll
    static void afterAll() throws Exception {
        if (vertx != null) {
            await(vertx.close());
        }
    }

    /** Request bodies of various sizes pass through {@code io.quarkus.vertx.http.runtime.VertxInputStream} */
    @ParameterizedTest
    @ValueSource(ints = { 1, 100_000, 5_000_000 })
    void requestSize(int length) {
        echo("h3Service", "x".repeat(length))
                .statusCode(200)
                .body(is("h3:" + length));
    }

    /** Response bodies of various sizes pass through {@code io.quarkiverse.cxf.transport.VertxServletOutputStream} */
    @ParameterizedTest
    @ValueSource(ints = { 1, 100_000, 5_000_000 })
    void responseSize(int length) {
        generate("h3Service", length)
                .statusCode(200)
                .body(is("h3:" + length));
    }

    /** An exception thrown by the service is delivered to the client as a SOAP fault */
    @Test
    void soapFault() {
        generate("h3Service", -1)
                .statusCode(500)
                .body(containsString("SOAPFaultException: length must not be negative: -1"));
    }

    /** The certificate presented by the client over QUIC is visible to the service through the servlet API */
    @Test
    void mutualTls() {
        echo("h3ServiceMutualTls", "x")
                .statusCode(200)
                /* ProtocolEchoImpl appends the subject of the client certificate */
                .body(is("h3:1:CN=localhost"));
    }

    /** The WSDL is served over HTTP/3 and the service address in it is the one through which the WSDL was requested */
    @Test
    void wsdl() throws Exception {
        final RawResponse response = request(HttpVersion.HTTP_3, HttpMethod.GET, SERVICE_PATH + "?wsdl", null);
        assertThat(response.version()).isEqualTo(HttpVersion.HTTP_3);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("<soap:address location=\"https://localhost:" + Http3Test.HTTPS_PORT + SERVICE_PATH + "\"/>");
    }

    /** The service can be called over HTTP/3 by a client other than Quarkus CXF */
    @Test
    void plainVertxClient() throws Exception {
        final RawResponse response = request(HttpVersion.HTTP_3, HttpMethod.POST, SERVICE_PATH, SOAP_REQUEST);
        assertThat(response.version()).isEqualTo(HttpVersion.HTTP_3);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.contentType()).startsWith("text/xml");
        assertThat(response.body()).contains("<return>h3:1</return>");
        /* There is no point in advertising HTTP/3 over HTTP/3 */
        assertThat(response.altSvc()).isNull();
    }

    /**
     * {@link HelloServiceImpl}, the example cited in the documentation, tells the HTTP version through which it was
     * called
     */
    @ParameterizedTest
    @CsvSource({ "HTTP_1_1,http/1.1", "HTTP_2,h2", "HTTP_3,h3" })
    void helloService(HttpVersion version, String expectedProtocol) throws Exception {
        final RawResponse response = request(version, HttpMethod.POST, "/services/hello",
                "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body>"
                        + "<ns2:hello xmlns:ns2=\"urn:hello\"><person>Joe</person></ns2:hello>"
                        + "</soap:Body></soap:Envelope>");
        assertThat(response.version()).isEqualTo(version);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("<return>Hello Joe over " + expectedProtocol + "</return>");
    }

    /** {@code quarkus-http3} advertises HTTP/3 via the {@code Alt-Svc} header also on the responses of Quarkus CXF */
    @Test
    @Disabled("https://github.com/quarkiverse/quarkus-cxf/issues/2476 - quarkus.http3.alt-svc = false is set in application.properties as a workaround")
    void altSvcOverHttp11() throws Exception {
        final RawResponse response = request(HttpVersion.HTTP_1_1, HttpMethod.POST, SERVICE_PATH, SOAP_REQUEST);
        assertThat(response.version()).isEqualTo(HttpVersion.HTTP_1_1);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("<return>http/1.1:1</return>");
        assertThat(response.altSvc()).startsWith("h3=\":" + Http3Test.HTTPS_PORT + "\"");
    }

    /**
     * A client having {@code vertx.http-versions = 3,2,1.1} negotiates HTTP/2 with {@link ProtocolEchoImpl} and it
     * stays on HTTP/2, because {@code quarkus-http3} advertises HTTP/3 only over HTTP/1.x as of Quarkus 4.0.0, see
     * https://github.com/quarkiverse/quarkus-cxf/issues/2446
     * <p>
     * Once that issue is solved, this test will start failing and it should be changed to expect the switch to HTTP/3,
     * like {@link Http3Test#http3AfterAltSvc(String, String)} does with {@link Http11Peer} and {@link Http3Peer}.
     */
    @Test
    void noHttp3AfterNegotiatingHttp2() throws InterruptedException {
        for (int i = 0; i < 5; i++) {
            echo("h3ServiceFallback", "x")
                    .statusCode(200)
                    .body(is("h2:1"));
            Thread.sleep(500);
        }
    }

    /**
     * Concurrent requests of a client that is allowed to open at most one HTTP/3 connection (the default of
     * {@code vertx.connection-pool.http3-max-size}) are served through a single connection.
     * <p>
     * {@link Http3PeerCustomizer} makes the server accept at most
     * {@value Http3PeerCustomizer#MAX_CONCURRENT_HTTP3_REQUESTS} concurrent requests per HTTP/3 connection, so some of
     * the requests have to wait for the others to complete.
     */
    @Test
    void concurrentRequests() {
        final int concurrentRequests = 3 * Http3PeerCustomizer.MAX_CONCURRENT_HTTP3_REQUESTS;
        final String connections = RestAssured.given()
                .queryParam("count", concurrentRequests)
                /* Hold each request long enough so that the requests overlap */
                .body("300")
                .post("/http3/connectionIds/h3Service")
                .then()
                .statusCode(200)
                .extract().body().asString();
        final List<String> perRequest = List.of(connections.split(","));
        assertThat(perRequest).hasSize(concurrentRequests).allMatch(connection -> connection.startsWith("h3:"));
        assertThat(Set.copyOf(perRequest)).hasSize(1);
    }

    private static ValidatableResponse echo(String client, String body) {
        return RestAssured.given()
                .body(body)
                .post("/http3/echo/" + client)
                .then();
    }

    private static ValidatableResponse generate(String client, int length) {
        return RestAssured.given()
                .body(String.valueOf(length))
                .post("/http3/generate/" + client)
                .then();
    }

    /**
     * Sends a single request to the HTTPS port of the application using a plain Vert.x HTTP client.
     *
     * @param version the only HTTP version the client is allowed to use
     * @param method the HTTP method
     * @param uri the path and query of the request
     * @param body the request body or {@code null} to send no body
     * @return the response
     */
    private static RawResponse request(HttpVersion version, HttpMethod method, String uri, String body) throws Exception {
        final HttpClientOptions options = new HttpClientOptions()
                .setSsl(true)
                .setTrustOptions(new PfxOptions().setPath("target/classes/localhost-truststore.p12").setPassword("secret"));
        if (version != HttpVersion.HTTP_3) {
            options.setProtocolVersion(version).setAlpnVersions(List.of(version)).setUseAlpn(true);
        }
        final HttpClientConfig config = new HttpClientConfig(options)
                .setVersions(List.of(version))
                .setFollowAlternativeServices(false);
        final HttpClientAgent client = vertx.httpClientBuilder().with(options).with(config).build();
        try {
            return await(client
                    .request(new RequestOptions()
                            .setMethod(method)
                            .setHost("localhost")
                            .setPort(Http3Test.HTTPS_PORT)
                            .setURI(uri)
                            .putHeader("Content-Type", "text/xml; charset=UTF-8"))
                    .compose(request -> body == null ? request.send() : request.send(body))
                    .compose(response -> response.body()
                            .map(responseBody -> new RawResponse(
                                    response.version(),
                                    response.statusCode(),
                                    response.getHeader("Content-Type"),
                                    response.getHeader("Alt-Svc"),
                                    responseBody.toString()))));
        } finally {
            await(client.close());
        }
    }

    private static <T> T await(Future<T> future) throws Exception {
        return future.toCompletionStage().toCompletableFuture().get(20, TimeUnit.SECONDS);
    }

    /**
     * A response received by a plain Vert.x HTTP client.
     *
     * @param version the HTTP version through which the response came
     * @param statusCode the HTTP status code
     * @param contentType the value of the {@code Content-Type} header or {@code null} if there was none
     * @param altSvc the value of the {@code Alt-Svc} header or {@code null} if there was none
     * @param body the response body
     */
    private record RawResponse(HttpVersion version, int statusCode, String contentType, String altSvc, String body) {
    }
}
