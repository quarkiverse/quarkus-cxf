package io.quarkiverse.cxf.it.http3;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
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
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.PfxOptions;
import io.vertx.core.net.ProxyOptions;
import io.vertx.core.net.ProxyType;

/**
 * Quarkus CXF rejects SOAP clients having {@code 3} in
 * {@code quarkus.cxf.client."client-name".vertx.http-versions} whose destination would be accessed through a proxy,
 * because Vert.x {@link io.vertx.core.http.HttpClient} would silently do something else than what was configured.
 * <p>
 * This test does not test Quarkus CXF. It documents the behavior of Vert.x {@link io.vertx.core.http.HttpClient} that
 * justifies the above mentioned checks in {@code io.quarkiverse.cxf.CXFClientInfo.validateHttp3Destination()}.
 * If this test starts failing after a Vert.x upgrade, then Vert.x has changed the way how it combines HTTP/3 and
 * proxies and the checks in Quarkus CXF should be reviewed and perhaps relaxed or removed.
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
public class VertxHttp3ProxyTest {

    private static final String SOAP_REQUEST = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body>"
            + "<ns2:echo xmlns:ns2=\"urn:protocol-echo\"><body>x</body></ns2:echo>"
            + "</soap:Body></soap:Envelope>";

    private static Vertx vertx;
    private static HttpServer proxy;
    /** The number of tunnels opened through {@link #proxy} */
    private static final AtomicInteger proxyTunnels = new AtomicInteger();

    @BeforeAll
    static void beforeAll() throws Exception {
        vertx = Vertx.vertx();
        final NetClient forwarder = vertx.createNetClient();
        /* A minimal HTTP proxy supporting only CONNECT tunnels */
        proxy = await(vertx.createHttpServer()
                .requestHandler(request -> {
                    if (request.method() != HttpMethod.CONNECT) {
                        request.response().setStatusCode(405).end();
                        return;
                    }
                    final String[] hostPort = request.uri().split(":");
                    forwarder.connect(Integer.parseInt(hostPort[1]), hostPort[0])
                            .onSuccess(target -> request.toNetSocket()
                                    .onSuccess(source -> {
                                        proxyTunnels.incrementAndGet();
                                        source.closeHandler(v -> target.close());
                                        target.closeHandler(v -> source.close());
                                        source.pipeTo(target);
                                        target.pipeTo(source);
                                    }))
                            .onFailure(e -> request.response().setStatusCode(502).end());
                })
                .listen(0, "localhost"));
    }

    @AfterAll
    static void afterAll() throws Exception {
        if (vertx != null) {
            await(vertx.close());
        }
    }

    /**
     * With HTTP/3 as the only version, Vert.x connects to the service directly, thus ignoring the proxy.
     */
    @Test
    void http3OnlyBypassesProxy() throws Exception {
        final HttpClientAgent client = client(List.of(HttpVersion.HTTP_3), true);
        try {
            final int tunnelsBefore = proxyTunnels.get();
            for (int i = 0; i < 3; i++) {
                assertThat(echo(client, "localhost")).contains("<return>HTTP/3.0:1</return>");
            }
            assertThat(proxyTunnels.get())
                    .as("Vert.x HttpClient is expected to bypass the proxy when HTTP/3 is the only permitted version")
                    .isEqualTo(tunnelsBefore);
        } finally {
            await(client.close());
        }
    }

    /**
     * With HTTP/3 combined with other versions, Vert.x does not follow alternative services advertised through a proxy,
     * hence it never uses HTTP/3.
     */
    @Test
    void http3WithFallbackNeverUsedThroughProxy() throws Exception {
        final List<HttpVersion> versions = List.of(HttpVersion.HTTP_3, HttpVersion.HTTP_1_1);

        /* First make sure that the given setup results in HTTP/3 being used when there is no proxy */
        final HttpClientAgent directClient = client(versions, false);
        try {
            assertThat(echo(directClient, "127.0.0.1")).contains("<return>HTTP/1.1:1</return>");
            Awaitility.await()
                    .atMost(Duration.ofSeconds(30))
                    .pollInterval(Duration.ofMillis(200))
                    .untilAsserted(() -> assertThat(echo(directClient, "127.0.0.1")).contains("<return>HTTP/3.0:1</return>"));
        } finally {
            await(directClient.close());
        }

        final HttpClientAgent proxiedClient = client(versions, true);
        try {
            final int tunnelsBefore = proxyTunnels.get();
            for (int i = 0; i < 6; i++) {
                assertThat(echo(proxiedClient, "127.0.0.1"))
                        .as("Vert.x HttpClient is expected not to switch to HTTP/3 when accessing the service through a proxy")
                        .contains("<return>HTTP/1.1:1</return>");
                Thread.sleep(500);
            }
            assertThat(proxyTunnels.get()).isGreaterThan(tunnelsBefore);
        } finally {
            await(proxiedClient.close());
        }
    }

    /**
     * Creates a client in the same way as {@code io.quarkiverse.cxf.vertx.http.client.HttpClientPool} does
     */
    private static HttpClientAgent client(List<HttpVersion> versions, boolean useProxy) {
        final HttpClientOptions options = new HttpClientOptions()
                .setSsl(true)
                .setTrustOptions(new PfxOptions().setPath("target/classes/localhost-truststore.p12").setPassword("secret"));
        if (useProxy) {
            options.setProxyOptions(new ProxyOptions()
                    .setType(ProxyType.HTTP)
                    .setHost("localhost")
                    .setPort(proxy.actualPort()));
        }
        final List<HttpVersion> tcpVersions = versions.stream().filter(v -> v != HttpVersion.HTTP_3).toList();
        if (!tcpVersions.isEmpty()) {
            options.setProtocolVersion(tcpVersions.get(0)).setAlpnVersions(tcpVersions).setUseAlpn(true);
        }
        final HttpClientConfig config = new HttpClientConfig(options)
                .setVersions(versions)
                .setFollowAlternativeServices(versions.size() > 1);
        return vertx.httpClientBuilder().with(options).with(config).build();
    }

    private static String echo(HttpClientAgent client, String host) throws Exception {
        return await(client
                .request(new RequestOptions()
                        .setMethod(HttpMethod.POST)
                        .setHost(host)
                        .setPort(Http3Test.HTTPS_PORT)
                        .setURI("/peer/echo")
                        .putHeader("Content-Type", "text/xml; charset=UTF-8"))
                .compose(request -> request.send(SOAP_REQUEST))
                .compose(response -> response.body())
                .map(body -> body.toString()));
    }

    private static <T> T await(Future<T> future) throws Exception {
        return future.toCompletionStage().toCompletableFuture().get(20, TimeUnit.SECONDS);
    }
}
