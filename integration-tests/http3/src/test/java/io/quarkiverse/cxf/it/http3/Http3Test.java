package io.quarkiverse.cxf.it.http3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ValidatableResponse;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Alias;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;

/**
 * SOAP clients having {@code 3} in {@code quarkus.cxf.client."client-name".vertx.http-versions} calling
 * {@link Http3Peer} served by the HTTP server of the application under test.
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
public class Http3Test {

    static final int HTTP_PORT = 8081;
    static final int HTTPS_PORT = 8444;

    /** {@code 3}: HTTP/3 for every request, including the first one, even though the host name has no dot */
    @Test
    void http3Only() {
        for (int i = 0; i < 3; i++) {
            echo("h3", "x".repeat(100_000))
                    .statusCode(200)
                    .body(is("h3:100000"));
        }
    }

    /**
     * {@code 3,2,1.1}: the first request goes over TCP and the client switches to HTTP/3 only after the service
     * advertised it and after Vert.x probed the advertised alternative successfully.
     * <ul>
     * <li>{@code h3Fallback} calls {@link Http11Peer}, that supports only HTTP/1.1 and advertises HTTP/3 via the
     * {@code Alt-Svc} response header
     * <li>{@code h3FallbackH2} calls {@link Http3Peer}, that advertises HTTP/3 via the HTTP/2 {@code ALTSVC} frame
     * </ul>
     */
    @ParameterizedTest
    @CsvSource({ "h3Fallback,http/1.1", "h3FallbackH2,h2" })
    void http3AfterAltSvc(String client, String firstProtocol) {
        echo(client, "first")
                .statusCode(200)
                .body(is(firstProtocol + ":5"));

        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> echo(client, "retry")
                        .statusCode(200)
                        .body(is("h3:5")));

        for (int i = 0; i < 3; i++) {
            echo(client, "x".repeat(100_000))
                    .statusCode(200)
                    .body(is("h3:100000"));
        }
    }

    /**
     * Vert.x does not follow the alternative services advertised by hosts whose name does not contain a dot.
     * {@code h3FallbackH2} and {@code h3FallbackLocalhost} differ only in their host names.
     */
    @Test
    void noHttp3AfterAltSvcWithoutDotInHostName() throws InterruptedException {
        for (int i = 0; i < 5; i++) {
            echo("h3FallbackLocalhost", "x")
                    .statusCode(200)
                    .body(is("h2:1"));
            Thread.sleep(500);
        }
    }

    /** The key store of the client is honored for QUIC connections */
    @Test
    void mutualTls() {
        echo("h3MutualTls", "x")
                .statusCode(200)
                /* Http3Peer appends the subject of the client certificate */
                .body(is("h3:1:CN=localhost"));
    }

    @Test
    void trustAll() {
        echo("h3TrustAll", "x")
                .statusCode(200)
                .body(is("h3:1"));
    }

    /** The trust store of the client is honored for QUIC connections */
    @Test
    void untrusted() {
        echo("h3Untrusted", "x")
                .statusCode(500)
                .body(anyOf(
                        containsString("CERTIFICATE_VERIFY_FAILED"),
                        containsString("QUICHE_ERR_TLS_FAIL")));
    }

    @Test
    void receiveTimeout() {
        RestAssured.given()
                .body("100")
                .post("/http3/delayedEcho/h3ReceiveTimeout")
                .then()
                .statusCode(200)
                .body(is("h3"));

        RestAssured.given()
                .body("3000")
                .post("/http3/delayedEcho/h3ReceiveTimeout")
                .then()
                .statusCode(500)
                .body(containsString("Timeout waiting 500 ms to receive response headers from https://localhost:"
                        + HTTPS_PORT + "/peer/echo"));
    }

    /**
     * {@code quarkus.cxf.client."client-name".connection-timeout} is honored by clients using HTTP/3.
     * <p>
     * What this test covers: the connection timeout which {@code VertxHttpClientHTTPConduit} passes to Vert.x with each
     * request via {@code RequestOptions.connectTimeout}, in the same way as it does for HTTP/1.x and HTTP/2.
     * Vert.x uses it to limit the time for getting a connection from its pool, which is where the asserted message
     * comes from.
     * <p>
     * What this test does not cover: the QUIC connect timeout set via {@code QuicClientConfig.setConnectTimeout()} in
     * {@code HttpClientPool.toHttp3Config()}.
     * Both timeouts are set to the same value here and the one passed with the request is the one that makes the call
     * fail, so this test passes even without the QUIC connect timeout being set.
     * {@code Http3ClientConfigTest.timeouts()} checks only that the value reaches the QUIC configuration.
     * The QUIC connect timeout becomes visible only if the two differ: when {@code HTTPClientPolicy.connectionTimeout}
     * is set programmatically to a value higher than {@code connection-timeout}, the connection attempt still fails
     * after {@code connection-timeout} with a {@code io.netty.channel.ConnectTimeoutException}.
     */
    @Test
    void connectionTimeout() {
        final long start = System.nanoTime();
        echo("h3ConnectionTimeout", "x")
                .statusCode(500)
                .body(containsString("timeout of 500 ms has been exceeded when getting a connection to localhost:9"));
        /* Without any connection timeout, the call would fail only after 60 seconds, the default of Vert.x */
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(20));
    }

    /**
     * {@code quarkus.cxf.client."client-name".vertx.http3-keep-alive-timeout} makes the client close idle HTTP/3
     * connections, whereas {@code vertx.keep-alive-timeout} and {@code vertx.http2-keep-alive-timeout} have no effect on
     * them
     */
    @Test
    void http3KeepAliveTimeout() throws InterruptedException {
        /* http3-keep-alive-timeout = 1 */
        final String shortTimeoutConnection = connectionId("h3KeepAliveTimeout");
        /* keep-alive-timeout = 1 and http2-keep-alive-timeout = 1, http3-keep-alive-timeout left at its default 60 */
        final String otherTimeoutsConnection = connectionId("h3OtherKeepAliveTimeouts");
        assertThat(shortTimeoutConnection).isNotEqualTo(otherTimeoutsConnection);

        /* Both clients reuse their connections as long as those are not idle for too long */
        assertThat(connectionId("h3KeepAliveTimeout")).isEqualTo(shortTimeoutConnection);
        assertThat(connectionId("h3OtherKeepAliveTimeouts")).isEqualTo(otherTimeoutsConnection);

        /* The service sees the connection of the first client closed, without any further request being sent */
        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> assertThat(closedConnections()).contains(shortTimeoutConnection));

        /*
         * Wait long enough for the 1 second timeouts of the second client and the pool cleaner period (1 second by
         * default) to pass, so that we would notice if they were applied to its HTTP/3 connection
         */
        Thread.sleep(3_500);
        assertThat(closedConnections()).doesNotContain(otherTimeoutsConnection);
        assertThat(connectionId("h3OtherKeepAliveTimeouts")).isEqualTo(otherTimeoutsConnection);

        /* The first client opens a new connection */
        assertThat(connectionId("h3KeepAliveTimeout")).isNotEqualTo(shortTimeoutConnection);
    }

    /**
     * {@code quarkus.cxf.client."client-name".vertx.connection-pool.http3-max-size} limits the number of HTTP/3
     * connections opened by the client, whereas {@code http1-max-size} and {@code http2-max-size} have no effect on it.
     * <p>
     * {@link Http3PeerCustomizer} makes the service accept at most
     * {@value Http3PeerCustomizer#MAX_CONCURRENT_HTTP3_REQUESTS} concurrent requests per HTTP/3 connection.
     * Hence a client sending more concurrent requests opens as many connections as it is allowed to.
     */
    @ParameterizedTest
    @CsvSource({
            /* http3-max-size = 1, http1-max-size = 3, http2-max-size = 3 */
            "h3MaxSize1,1",
            /* http3-max-size = 3, http1-max-size = 1, http2-max-size = 1 */
            "h3MaxSize3,3" })
    void http3MaxSize(String client, int expectedConnections) {
        /* More requests than what the service accepts over the maximum number of connections of either client */
        final int concurrentRequests = 4 * Http3PeerCustomizer.MAX_CONCURRENT_HTTP3_REQUESTS;
        final String connections = RestAssured.given()
                .queryParam("count", concurrentRequests)
                /* Hold each request long enough so that the requests overlap */
                .body("700")
                .post("/http3/connectionIds/" + client)
                .then()
                .statusCode(200)
                .extract().body().asString();
        final List<String> perRequest = List.of(connections.split(","));
        assertThat(perRequest).hasSize(concurrentRequests).allMatch(connection -> connection.startsWith("h3:"));
        assertThat(Set.copyOf(perRequest)).hasSize(expectedConnections);
    }

    /**
     * {@code quarkus-cxf-rt-features-metrics} names the Vert.x HttpClient of each SOAP client through a
     * {@code HttpClientCustomizerBuildItem}, so that its metrics are tagged with the name of the SOAP client.
     * That has to work for clients using HTTP/3 too.
     */
    @Test
    void metrics() {
        final String activeConnections = "http.client.active.connections;clientName=h3Metrics";
        /* No other test uses the h3Metrics client, hence there are no metrics for it before we call it */
        assertThat(getMetrics().get(activeConnections)).isNull();

        for (int i = 0; i < 2; i++) {
            echo("h3Metrics", "x")
                    .statusCode(200)
                    .body(is("h3:1"));
        }

        final Map<String, Object> metrics = getMetrics();
        /* Both requests went through a single HTTP/3 connection that is still open */
        assertThat(metrics.get(activeConnections)).isEqualTo(1.0f);
        assertThat(metrics.get("http.client.requests"))
                .asInstanceOf(InstanceOfAssertFactories.map(String.class, Object.class))
                .containsEntry("count;address=localhost:" + HTTPS_PORT
                        + ";clientName=h3Metrics;method=POST;outcome=SUCCESS;status=200;uri=/peer/echo", 2);
    }

    @Test
    void redirect() {
        echo("h3Redirect", "x", "https://localhost:" + HTTPS_PORT + "/peer/redirect?to=https://localhost:" + HTTPS_PORT
                + "/peer/echo")
                .statusCode(200)
                .body(is("h3:1"));
    }

    /** HTTP/3 requires TLS, so a redirect to a plain HTTP URL has to fail rather than silently use something else */
    @Test
    void redirectToPlainHttp() {
        echo("h3Redirect", "x", "https://localhost:" + HTTPS_PORT + "/peer/redirect?to=http://localhost:" + HTTP_PORT
                + "/peer/echo")
                .statusCode(500)
                .body(containsString(
                        "quarkus.cxf.client.h3Redirect.vertx.http-versions = 3 cannot be used with http://localhost:"
                                + HTTP_PORT + ", because HTTP/3 requires an https: URL"));
    }

    /** The client is configured properly, but the endpoint URL is changed to a plain HTTP URL per request */
    @Test
    void endpointOverrideToPlainHttp() {
        echo("h3EndpointOverride", "x", "http://localhost:" + HTTP_PORT + "/peer/echo")
                .statusCode(500)
                .body(containsString(
                        "quarkus.cxf.client.h3EndpointOverride.vertx.http-versions = 3,2,1.1"
                                + " cannot be used with http://localhost:" + HTTP_PORT
                                + ", because HTTP/3 requires an https: URL"));
    }

    /**
     * @param client the name of the client to use
     * @return the identifier of the HTTP/3 connection through which the service received the request
     */
    private static String connectionId(String client) {
        final String connection = RestAssured.given()
                .queryParam("count", 1)
                .body("0")
                .post("/http3/connectionIds/" + client)
                .then()
                .statusCode(200)
                .extract().body().asString();
        assertThat(connection).startsWith("h3:");
        return connection.substring("h3:".length());
    }

    /** @return the identifiers of the connections which {@link Http3Peer} has seen closed */
    private static Set<String> closedConnections() {
        final String closed = RestAssured.get("/peer/closedConnections")
                .then()
                .statusCode(200)
                .extract().body().asString();
        return closed.isEmpty() ? Set.of() : Set.of(closed.split(","));
    }

    private static Map<String, Object> getMetrics() {
        final String body = RestAssured.given()
                .header("Content-Type", "application/json")
                .get("/q/metrics/json")
                .then()
                .statusCode(200)
                .extract().body().asString();
        return new JsonPath(body).getJsonObject("$");
    }

    private static ValidatableResponse echo(String client, String body) {
        return echo(client, body, null);
    }

    private static ValidatableResponse echo(String client, String body, String endpoint) {
        return (endpoint == null
                ? RestAssured.given()
                : RestAssured.given().queryParam("endpoint", endpoint))
                .body(body)
                .post("/http3/echo/" + client)
                .then();
    }
}
