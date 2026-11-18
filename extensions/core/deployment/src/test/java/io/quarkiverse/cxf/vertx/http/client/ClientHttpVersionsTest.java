package io.quarkiverse.cxf.vertx.http.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.WebServiceException;

import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.transport.http.HTTPConduit;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.core.Vertx;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerConfig;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.PfxOptions;
import io.vertx.core.net.ServerSSLOptions;

@Certificates(baseDir = "target/classes", replaceIfExists = true, certificates = {
        @Certificate(name = "localhost", password = "secret", formats = Format.PKCS12),
        @Certificate(name = "server.invalid", cn = "server.invalid", subjectAlternativeNames = "DNS:server.invalid", password = "secret", formats = Format.PKCS12)
})
public class ClientHttpVersionsTest {
    @RegisterExtension
    public static final QuarkusExtensionTest test = createTest();

    static QuarkusExtensionTest createTest() {
        QuarkusExtensionTest test = new QuarkusExtensionTest()
                .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(VersionService.class, Servers.class));
        Map<String, String> lists = Map.ofEntries(
                Map.entry("ordered", "2,1.1"), Map.entry("reversed", "1.1,2"),
                Map.entry("http2", "2"), Map.entry("http11", "1.1"), Map.entry("http10", "1.0"),
                Map.entry("forcedHttp2", "2"), Map.entry("forcedHttp11", "1.1"),
                Map.entry("wrongHost", "2,1.1"), Map.entry("noVerification", "2,1.1"),
                Map.entry("untrusted", "2,1.1"), Map.entry("urlConnection", "2,1.1"),
                Map.entry("orderedClear", "2,1.1"), Map.entry("http2Clear", "2"), Map.entry("http10Clear", "1.0"),
                Map.entry("chunked", "2,1.1"), Map.entry("closing", "2,1.1"), Map.entry("closingClear", "2,1.1"),
                Map.entry("reusing", "2,1.1"), Map.entry("preflightTimeout", "2,1.1"),
                Map.entry("legacyClear", ""), Map.entry("legacyHttp2Clear", ""), Map.entry("legacyHttp2Closing", ""),
                Map.entry("legacy", ""), Map.entry("legacyHttp2", ""));
        lists.forEach((name, versions) -> {
            String key = "quarkus.cxf.client." + name;
            test.overrideConfigKey(key + ".service-interface", VersionService.class.getName())
                    .overrideConfigKey(key + ".client-endpoint-url", "http://localhost:8081/unused")
                    .overrideConfigKey(key + ".http-conduit-factory", "VertxHttpClientHTTPConduitFactory")
                    .overrideConfigKey(key + ".connection-timeout", "3000")
                    .overrideConfigKey(key + ".receive-timeout", "3000");
            if (!versions.isEmpty()) {
                test.overrideConfigKey(key + ".vertx.versions", versions);
            }
            if (!name.equals("untrusted")) {
                String tls = name.startsWith("forced") ? "forced" : switch (name) {
                    case "wrongHost", "noVerification" -> name;
                    default -> "trusted";
                };
                test.overrideConfigKey(key + ".tls-configuration-name", tls);
            }
        });
        test.overrideConfigKey("quarkus.cxf.client.legacyHttp2.version", "2")
                .overrideConfigKey("quarkus.cxf.client.ordered.auto-redirect", "true")
                .overrideConfigKey("quarkus.cxf.client.chunked.chunking-threshold", "1")
                .overrideConfigKey("quarkus.cxf.client.closing.connection", "close")
                .overrideConfigKey("quarkus.cxf.client.closingClear.connection", "close")
                .overrideConfigKey("quarkus.cxf.client.legacyHttp2Clear.version", "2")
                .overrideConfigKey("quarkus.cxf.client.legacyHttp2Closing.version", "2")
                .overrideConfigKey("quarkus.cxf.client.legacyHttp2Closing.connection", "close")
                .overrideConfigKey("quarkus.cxf.client.reusing.vertx.connection-pool.http1-max-size", "1")
                .overrideConfigKey("quarkus.cxf.client.preflightTimeout.connection-timeout", "1000")
                .overrideConfigKey("quarkus.cxf.client.urlConnection.http-conduit-factory", "URLConnectionHTTPConduitFactory")
                .overrideConfigKey("quarkus.tls.forced.key-store.p12.path", "localhost-keystore.p12")
                .overrideConfigKey("quarkus.tls.forced.key-store.p12.password", "secret")
                .overrideConfigKey("quarkus.tls.forced.key-store.sni", "true")
                .overrideConfigKey("quarkus.tls.noVerification.hostname-verification-algorithm", "NONE");
        for (String name : List.of("trusted", "forced", "wrongHost", "noVerification")) {
            String certificate = name.equals("wrongHost") || name.equals("noVerification") ? "server.invalid" : "localhost";
            test.overrideConfigKey("quarkus.tls." + name + ".trust-store.p12.path", certificate + "-truststore.p12")
                    .overrideConfigKey("quarkus.tls." + name + ".trust-store.p12.password", "secret");
        }
        return test;
    }

    @Inject
    Servers servers;

    @CXFClient("ordered")
    VersionService ordered;
    @CXFClient("reversed")
    VersionService reversed;
    @CXFClient("http2")
    VersionService http2;
    @CXFClient("http11")
    VersionService http11;
    @CXFClient("http10")
    VersionService http10;
    @CXFClient("orderedClear")
    VersionService orderedClear;
    @CXFClient("http2Clear")
    VersionService http2Clear;
    @CXFClient("http10Clear")
    VersionService http10Clear;
    @CXFClient("legacyClear")
    VersionService legacyClear;
    @CXFClient("legacyHttp2Clear")
    VersionService legacyHttp2Clear;
    @CXFClient("legacyHttp2Closing")
    VersionService legacyHttp2Closing;
    @CXFClient("reusing")
    VersionService reusing;
    @CXFClient("preflightTimeout")
    VersionService preflightTimeout;
    @CXFClient("chunked")
    VersionService chunked;
    @CXFClient("closing")
    VersionService closing;
    @CXFClient("closingClear")
    VersionService closingClear;
    @CXFClient("legacy")
    VersionService legacy;
    @CXFClient("legacyHttp2")
    VersionService legacyHttp2;
    @CXFClient("forcedHttp2")
    VersionService forcedHttp2;
    @CXFClient("forcedHttp11")
    VersionService forcedHttp11;
    @CXFClient("wrongHost")
    VersionService wrongHost;
    @CXFClient("noVerification")
    VersionService noVerification;
    @CXFClient("untrusted")
    VersionService untrusted;
    @CXFClient("urlConnection")
    jakarta.enterprise.inject.Instance<VersionService> urlConnection;

    @Test
    void tlsNegotiationAndFallback() {
        assertThat(invoke(ordered, servers.tlsHttp2, true)).startsWith("HTTP_2|null|");
        assertThat(invoke(ordered, servers.tlsHttp1, true)).startsWith("HTTP_1_1|keep-alive|");
        assertThat(invoke(http2, servers.tlsHttp2, true)).startsWith("HTTP_2|null|");
        assertThatThrownBy(() -> invoke(http2, servers.tlsHttp1, true)).isInstanceOf(WebServiceException.class);
        assertThat(invoke(http11, servers.tlsBoth, true)).startsWith("HTTP_1_1|keep-alive|");
        assertThatThrownBy(() -> invoke(http11, servers.tlsHttp2, true)).isInstanceOf(WebServiceException.class);
        assertThat(invoke(http10, servers.tlsHttp1, true)).startsWith("HTTP_1_0|keep-alive|");
    }

    @Test
    void cleartextOrderUpgradeAndStrictSelection() {
        assertThat(invoke(orderedClear, servers.clearBoth, false)).startsWith("HTTP_2|null|");
        assertThat(invoke(reversed, servers.clearBoth, false)).startsWith("HTTP_1_1|keep-alive|");
        assertThat(invoke(orderedClear, servers.clearHttp1, false)).startsWith("HTTP_1_1|keep-alive|");
        assertThat(invoke(http2Clear, servers.clearBoth, false)).startsWith("HTTP_2|null|");
        assertThatThrownBy(() -> invoke(http2Clear, servers.clearHttp1, false)).isInstanceOf(WebServiceException.class);
        assertThat(invoke(http10Clear, servers.clearHttp1, false)).startsWith("HTTP_1_0|keep-alive|");
    }

    @Test
    void existingDefaultsAndFallback() {
        assertThat(invoke(legacy, servers.tlsBoth, true)).startsWith("HTTP_1_1|keep-alive|");
        assertThat(invoke(legacyClear, servers.clearBoth, false)).startsWith("HTTP_1_1|keep-alive|");
        assertThat(invoke(legacyHttp2, servers.tlsHttp2, true)).startsWith("HTTP_2|null|");
        assertThat(invoke(legacyHttp2, servers.tlsHttp1, true)).startsWith("HTTP_1_1|null|");
        assertThat(invoke(legacyHttp2Clear, servers.clearHttp1, false)).startsWith("HTTP_1_1|http2-settings,upgrade|");
    }

    @Test
    void forcedSniAndMutualTls() {
        assertThat(invoke(forcedHttp2, servers.tlsHttp2, true)).isEqualTo("HTTP_2|null|localhost");
        assertThat(invoke(forcedHttp11, servers.tlsHttp1, true)).isEqualTo("HTTP_1_1|keep-alive|localhost");
        assertThat(invoke(forcedHttp2, servers.mutualTls, true)).isEqualTo("HTTP_2|null|localhost");
        assertThatThrownBy(() -> invoke(http2, servers.mutualTls, true)).isInstanceOf(WebServiceException.class);
    }

    @Test
    void trustAndHostnameVerification() {
        assertThatThrownBy(() -> invoke(untrusted, servers.tlsHttp2, true))
                .hasStackTraceContaining("PKIX path building failed");
        assertThatThrownBy(() -> invoke(wrongHost, servers.wrongHost, true))
                .hasStackTraceContaining("No subject alternative DNS name matching localhost");
        assertThat(invoke(noVerification, servers.wrongHost, true)).startsWith("HTTP_2|null|");
    }

    @Test
    void rejectsOtherConduits() {
        assertThatThrownBy(() -> urlConnection.get().inspect("")).hasStackTraceContaining("vertx.versions requires");
    }

    @Test
    void redirectNegotiatesHeadersForEachDestination() {
        setAddress(ordered, servers.tlsHttp2, true, "/redirect-http1");
        assertThat(ordered.inspect("")).startsWith("HTTP_1_1|keep-alive|");
        setAddress(ordered, servers.tlsHttp1, true, "/redirect-http2");
        assertThat(ordered.inspect("")).startsWith("HTTP_2|null|");
    }

    @Test
    void chunkedBodyAfterCleartextNegotiation() {
        setAddress(chunked, servers.clearBoth, false, "/inspect");
        assertThat(chunked.inspect("x".repeat(20_000))).startsWith("HTTP_2|null|");
        setAddress(chunked, servers.clearHttp1, false, "/inspect");
        assertThat(chunked.inspect("x".repeat(20_000))).startsWith("HTTP_1_1|keep-alive|");
    }

    @Test
    void connectionCloseHeaderOnlyForHttp1() {
        assertThat(invoke(closing, servers.tlsHttp1, true)).startsWith("HTTP_1_1|close|");
        assertThat(invoke(closing, servers.tlsHttp2, true)).startsWith("HTTP_2|null|");
    }

    @Test
    void connectionCloseWithCleartextUpgrade() {
        assertThat(invoke(closingClear, servers.clearBoth, false)).startsWith("HTTP_2|null|");
    }

    @Test
    void connectionCloseWithCleartextFallback() {
        assertThat(invoke(closingClear, servers.clearHttp1, false)).startsWith("HTTP_1_1|close|");
    }

    @Test
    void legacyConnectionCloseWithCleartextUpgrade() {
        assertThat(invoke(legacyHttp2Closing, servers.clearBoth, false)).startsWith("HTTP_2|null|");
    }

    @ParameterizedTest
    @ValueSource(strings = { "keep-alive", "close", "chunked" })
    void cleartextFallbackAfterPreflightClosesConnection(String mode) {
        final VersionService client = switch (mode) {
            case "close" -> closingClear;
            case "chunked" -> chunked;
            default -> orderedClear;
        };
        final int preflights = servers.closingPreflights.get();
        final int soapRequests = servers.closingSoapRequests.get();
        setAddress(client, servers.clearClosing, false, "/inspect");
        assertThat(client.inspect("chunked".equals(mode) ? "x".repeat(20_000) : ""))
                .startsWith("HTTP_1_1|" + ("close".equals(mode) ? "close" : "keep-alive") + "|");
        assertThat(servers.closingPreflights.get()).isEqualTo(preflights + 1);
        assertThat(servers.closingSoapRequests.get()).isEqualTo(soapRequests + 1);
    }

    @Test
    void cleartextFallbackAfterRedirectPreflightClosesConnection() {
        final int preflights = servers.closingPreflights.get();
        final int soapRequests = servers.closingSoapRequests.get();
        setAddress(ordered, servers.tlsHttp2, true, "/redirect-closing");
        assertThat(ordered.inspect("")).startsWith("HTTP_1_1|keep-alive|");
        assertThat(servers.closingPreflights.get()).isEqualTo(preflights + 1);
        assertThat(servers.closingSoapRequests.get()).isEqualTo(soapRequests + 1);
    }

    @Test
    void doesNotRetryAfterSendingSoap() {
        final int soapRequests = servers.closingSoapRequests.get();
        setAddress(orderedClear, servers.clearClosing, false, "/drop-response");
        assertThatThrownBy(() -> orderedClear.inspect("")).isInstanceOf(WebServiceException.class);
        assertThat(servers.closingSoapRequests.get()).isEqualTo(soapRequests + 1);
    }

    @Test
    void preflightIsNotRepeatedOnReusedConnections() {
        final int http1Preflights = servers.http1Preflights.get();
        assertThat(invoke(reusing, servers.clearHttp1, false)).startsWith("HTTP_1_1|keep-alive|");
        assertThat(invoke(reusing, servers.clearHttp1, false)).startsWith("HTTP_1_1|keep-alive|");
        assertThat(servers.http1Preflights.get()).isEqualTo(http1Preflights + 1);

        final int http2Preflights = servers.http2Preflights.get();
        assertThat(invoke(reusing, servers.clearBoth, false)).startsWith("HTTP_2|null|");
        assertThat(invoke(reusing, servers.clearBoth, false)).startsWith("HTTP_2|null|");
        assertThat(servers.http2Preflights.get()).isEqualTo(http2Preflights + 1);
    }

    @Test
    void preflightTimeoutDoesNotSendSoap() {
        assertThatThrownBy(() -> invoke(preflightTimeout, servers.clearStalled, false))
                .hasStackTraceContaining("Timeout");
        assertThat(servers.stalledPreflights.get()).isEqualTo(1);
        assertThat(servers.stalledSoapRequests.get()).isZero();
    }

    @Test
    void rejectsRequestVersionOverrides() {
        BindingProvider provider = (BindingProvider) ordered;
        provider.getRequestContext().put("org.apache.cxf.transport.http.forceVersion", "1.1");
        try {
            assertThatThrownBy(() -> invoke(ordered, servers.clearBoth, false))
                    .hasStackTraceContaining("vertx.versions cannot be combined with a CXF HTTP version override");
        } finally {
            provider.getRequestContext().remove("org.apache.cxf.transport.http.forceVersion");
        }
        HTTPConduit conduit = (HTTPConduit) ClientProxy.getClient(ordered).getConduit();
        conduit.getClient().setVersion("1.1");
        try {
            assertThatThrownBy(() -> invoke(ordered, servers.clearBoth, false))
                    .hasStackTraceContaining("vertx.versions cannot be combined with a CXF HTTP version override");
        } finally {
            conduit.getClient().setVersion("auto");
        }
    }

    private static String invoke(VersionService client, HttpServer server, boolean tls) {
        setAddress(client, server, tls, "/inspect");
        return client.inspect("");
    }

    private static void setAddress(VersionService client, HttpServer server, boolean tls, String path) {
        ((BindingProvider) client).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                (tls ? "https" : "http") + "://localhost:" + server.actualPort() + path);
    }

    @WebService(targetNamespace = "https://quarkiverse.io/cxf/versions")
    public interface VersionService {
        @WebMethod
        String inspect(String payload);
    }

    @Singleton
    public static class Servers {
        final AtomicInteger closingPreflights = new AtomicInteger();
        final AtomicInteger closingSoapRequests = new AtomicInteger();
        final AtomicInteger http1Preflights = new AtomicInteger();
        final AtomicInteger http2Preflights = new AtomicInteger();
        final AtomicInteger stalledPreflights = new AtomicInteger();
        final AtomicInteger stalledSoapRequests = new AtomicInteger();
        HttpServer clearClosing;
        HttpServer clearStalled;
        HttpServer clearHttp1;
        HttpServer clearBoth;
        HttpServer tlsHttp1;
        HttpServer tlsHttp2;
        HttpServer tlsBoth;
        HttpServer wrongHost;
        HttpServer mutualTls;

        void start(@Observes StartupEvent event, Vertx vertx) throws Exception {
            clearClosing = start(vertx, null, false, false, HttpVersion.HTTP_1_1);
            clearStalled = start(vertx, null, false, false, HttpVersion.HTTP_1_1);
            clearHttp1 = start(vertx, null, false, false, HttpVersion.HTTP_1_1, HttpVersion.HTTP_1_0);
            clearBoth = start(vertx, null, false, false, HttpVersion.HTTP_2, HttpVersion.HTTP_1_1);
            tlsHttp1 = start(vertx, "localhost", false, false, HttpVersion.HTTP_1_1, HttpVersion.HTTP_1_0);
            tlsHttp2 = start(vertx, "localhost", true, false, HttpVersion.HTTP_2);
            tlsBoth = start(vertx, "localhost", true, false, HttpVersion.HTTP_2, HttpVersion.HTTP_1_1);
            wrongHost = start(vertx, "server.invalid", true, false, HttpVersion.HTTP_2, HttpVersion.HTTP_1_1);
            mutualTls = start(vertx, "localhost", true, true, HttpVersion.HTTP_2);
        }

        private HttpServer start(Vertx vertx, String certificate, boolean alpn, boolean mutual,
                HttpVersion... versions) throws Exception {
            HttpServerConfig config = new HttpServerConfig().setHost("localhost").setPort(0).setVersions(versions);
            config.getTcpConfig().setSsl(certificate != null);
            var builder = vertx.httpServerBuilder().with(config);
            if (certificate != null) {
                ServerSSLOptions ssl = new ServerSSLOptions().setUseAlpn(alpn).setSni(true)
                        .setKeyCertOptions(new PfxOptions().setPath(certificate + "-keystore.p12").setPassword("secret"));
                if (mutual) {
                    ssl.setClientAuth(ClientAuth.REQUIRED)
                            .setTrustOptions(new PfxOptions().setPath("localhost-truststore.p12").setPassword("secret"));
                }
                builder.with(ssl);
            }
            return builder.build().requestHandler(request -> request.bodyHandler(body -> {
                if (request.localAddress().port() == clearStalled.actualPort()) {
                    if (request.method() == HttpMethod.OPTIONS) {
                        stalledPreflights.incrementAndGet();
                    } else {
                        stalledSoapRequests.incrementAndGet();
                    }
                    return;
                }
                if (request.method() == HttpMethod.OPTIONS) {
                    if (request.localAddress().port() == clearHttp1.actualPort()) {
                        http1Preflights.incrementAndGet();
                    } else if (request.localAddress().port() == clearBoth.actualPort()) {
                        http2Preflights.incrementAndGet();
                    }
                }
                if (request.localAddress().port() == clearClosing.actualPort()) {
                    if (request.method() == HttpMethod.OPTIONS) {
                        closingPreflights.incrementAndGet();
                        request.response().setStatusCode(405).putHeader("Connection", "close").end();
                        return;
                    }
                    closingSoapRequests.incrementAndGet();
                    if (request.path().equals("/drop-response")) {
                        request.connection().close();
                        return;
                    }
                }
                if (request.path().equals("/redirect-closing")) {
                    request.response().setStatusCode(307)
                            .putHeader("Location", "http://localhost:" + clearClosing.actualPort() + "/inspect").end();
                    return;
                }
                if (request.path().startsWith("/redirect-")) {
                    HttpServer target = request.path().equals("/redirect-http1") ? tlsHttp1 : tlsHttp2;
                    request.response().setStatusCode(307)
                            .putHeader("Location", "https://localhost:" + target.actualPort() + "/inspect").end();
                    return;
                }
                String connection = request.getHeader("Connection");
                String result = request.version() + "|"
                        + (connection == null ? null : connection.toLowerCase(java.util.Locale.ROOT)) + "|"
                        + request.connection().indicatedServerName();
                request.response().putHeader("Content-Type", "text/xml; charset=UTF-8").end("""
                        <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                            <soap:Body>
                                <ns:inspectResponse xmlns:ns="https://quarkiverse.io/cxf/versions">
                                    <return>%s</return>
                                </ns:inspectResponse>
                            </soap:Body>
                        </soap:Envelope>
                        """.formatted(result));
            })).listen().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
        }
    }
}
