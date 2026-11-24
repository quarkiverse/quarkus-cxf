package io.quarkiverse.cxf.vertx.http.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.jws.WebService;
import jakarta.xml.ws.BindingProvider;

import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.transport.http.HTTPConduit;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.tls.CertificateUpdatedEvent;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.PfxOptions;
import io.vertx.core.net.ProxyOptions;
import io.vertx.core.net.ProxyType;

/** Real-peer acceptance tests for docs/specs/i2433/spec.md. */
@Certificates(baseDir = "target/classes", certificates = @Certificate(name = "ordered-localhost", cn = "localhost", subjectAlternativeNames = {
        "localhost" }, password = "secret", formats = Format.PKCS12))
public class OrderedHttpVersionsTest {
    private static final String PAYLOAD = "x".repeat(100_000);
    private static final String FORCE = "org.apache.cxf.transport.http.forceVersion";

    @RegisterExtension
    static final QuarkusExtensionTest test = configuration();

    private static QuarkusExtensionTest configuration() {
        final QuarkusExtensionTest result = new QuarkusExtensionTest()
                .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClass(Echo.class))
                .overrideConfigKey("quarkus.tls.ordered.trust-store.p12.path", "ordered-localhost-truststore.p12")
                .overrideConfigKey("quarkus.tls.ordered.trust-store.p12.password", "secret");
        configure(result, "h2", "2");
        configure(result, "prefer", "2,1.1");
        configure(result, "h10", "1.0");
        configure(result, "h11", "1.1");
        configure(result, "legacyDefault", null);
        configure(result, "legacy10", null);
        configure(result, "legacy11", null);
        configure(result, "legacy2", null);
        configure(result, "urlConnection", "2");
        configure(result, "unsupported", "1.1,2");
        configure(result, "untrusted", "2,1.1");
        result.overrideConfigKey("quarkus.cxf.client.legacy10.version", "1.0")
                .overrideConfigKey("quarkus.cxf.client.legacy11.version", "1.1")
                .overrideConfigKey("quarkus.cxf.client.legacy2.version", "2")
                .overrideConfigKey("quarkus.cxf.client.urlConnection.http-conduit-factory", "URLConnectionHTTPConduitFactory");
        configure(result, "tlsPrefer", "2,1.1");
        configure(result, "tlsH2", "2");
        configure(result, "tlsH10", "1.0");
        configure(result, "tlsH11", "1.1");
        configure(result, "tlsLegacy2", null);
        configure(result, "forceUpgrade", null);
        configure(result, "forceRefusal", null);
        configure(result, "enableUpgrade", null);
        configure(result, "enableRefusal", null);
        configure(result, "overrideForce", "2,1.1");
        configure(result, "overrideEnable", "1.1");
        configure(result, "overridePolicy", "2,1.1");
        result.overrideConfigKey("quarkus.cxf.client.tlsLegacy2.version", "2");
        configure(result, "lifecycle", "2,1.1");
        configure(result, "customized", "1.1");
        configure(result, "customDirect", "2");
        result.overrideConfigKey("quarkus.cxf.client.customized.auth.username", "alice")
                .overrideConfigKey("quarkus.cxf.client.customized.auth.password", "secret");
        return result;
    }

    private static void configure(QuarkusExtensionTest result, String name, String versions) {
        final String key = "quarkus.cxf.client." + name;
        result.overrideConfigKey(key + ".service-interface", Echo.class.getName())
                .overrideConfigKey(key + ".client-endpoint-url", "http://localhost:1/unused")
                .overrideConfigKey(key + ".http-conduit-factory", "VertxHttpClientHTTPConduitFactory")
                .overrideConfigKey(key + ".receive-timeout", "2000")
                .overrideConfigKey(key + ".connection-timeout", "2000")
                .overrideConfigKey(key + ".vertx.connection-pool.http1-max-size", "1");
        if (!name.equals("untrusted")) {
            result.overrideConfigKey(key + ".tls-configuration-name", "ordered");
        }
        if (name.equals("legacy10") || (versions != null && versions.startsWith("1.0"))) {
            result.overrideConfigKey(key + ".allow-chunking", "false");
        }
        if (versions != null) {
            result.overrideConfigKey(key + ".vertx.http-versions", versions);
        }
    }

    @CXFClient("h2")
    Instance<Echo> h2;

    @CXFClient("prefer")
    Instance<Echo> prefer;

    @CXFClient("h10")
    Instance<Echo> h10;

    @CXFClient("h11")
    Instance<Echo> h11;

    @CXFClient("legacyDefault")
    Instance<Echo> legacyDefault;

    @CXFClient("legacy10")
    Instance<Echo> legacy10;

    @CXFClient("legacy11")
    Instance<Echo> legacy11;

    @CXFClient("legacy2")
    Instance<Echo> legacy2;

    @CXFClient("urlConnection")
    Instance<Echo> urlConnection;

    @CXFClient("unsupported")
    Instance<Echo> unsupported;

    @CXFClient("untrusted")
    Instance<Echo> untrusted;

    @CXFClient("tlsPrefer")
    Instance<Echo> tlsPrefer;

    @CXFClient("tlsH2")
    Instance<Echo> tlsH2;

    @CXFClient("tlsH10")
    Instance<Echo> tlsH10;

    @CXFClient("tlsH11")
    Instance<Echo> tlsH11;

    @CXFClient("tlsLegacy2")
    Instance<Echo> tlsLegacy2;

    @CXFClient("forceUpgrade")
    Instance<Echo> forceUpgrade;

    @CXFClient("forceRefusal")
    Instance<Echo> forceRefusal;

    @CXFClient("enableUpgrade")
    Instance<Echo> enableUpgrade;

    @CXFClient("enableRefusal")
    Instance<Echo> enableRefusal;

    @CXFClient("overrideForce")
    Instance<Echo> overrideForce;

    @CXFClient("overrideEnable")
    Instance<Echo> overrideEnable;

    @CXFClient("overridePolicy")
    Instance<Echo> overridePolicy;

    @CXFClient("lifecycle")
    Instance<Echo> lifecycle;

    @CXFClient("customized")
    Instance<Echo> customized;

    @CXFClient("customDirect")
    Instance<Echo> customDirect;

    private Instance<Echo> client(String name) {
        return switch (name) {
            case "h2" -> h2;
            case "prefer" -> prefer;
            case "h10" -> h10;
            case "h11" -> h11;
            case "legacyDefault" -> legacyDefault;
            case "legacy10" -> legacy10;
            case "legacy11" -> legacy11;
            case "legacy2" -> legacy2;
            case "urlConnection" -> urlConnection;
            case "untrusted" -> untrusted;
            case "tlsPrefer" -> tlsPrefer;
            case "tlsH2" -> tlsH2;
            case "tlsLegacy2" -> tlsLegacy2;
            case "forceUpgrade" -> forceUpgrade;
            case "forceRefusal" -> forceRefusal;
            case "enableUpgrade" -> enableUpgrade;
            case "enableRefusal" -> enableRefusal;
            case "overrideForce" -> overrideForce;
            case "overrideEnable" -> overrideEnable;
            case "overridePolicy" -> overridePolicy;
            case "tlsH10" -> tlsH10;
            case "tlsH11" -> tlsH11;
            default -> throw new IllegalArgumentException(name);
        };
    }

    static Stream<Arguments> plaintext() {
        return Stream.of(
                Arguments.of("h2", true, "HTTP_2", false),
                Arguments.of("prefer", true, "HTTP_2", true),
                Arguments.of("prefer", false, "HTTP_1_1", true),
                Arguments.of("h10", true, "HTTP_1_0", false),
                Arguments.of("h11", true, "HTTP_1_1", false),
                Arguments.of("legacyDefault", true, "HTTP_1_1", false),
                Arguments.of("legacy10", true, "HTTP_1_0", false),
                Arguments.of("legacy11", true, "HTTP_1_1", false),
                Arguments.of("legacy2", true, "HTTP_2", true),
                Arguments.of("legacy2", false, "HTTP_1_1", true));
    }

    @ParameterizedTest(name = "{0}, h2c={1}")
    @MethodSource("plaintext")
    void plaintextSelectionAndReuse(String name, boolean h2c, String expected, boolean preflight) throws Exception {
        assumeFalse(name.equals("h11"),
                "https://github.com/quarkiverse/quarkus-cxf/issues/2438 - cached HTTPS settings persist on HTTP requests");
        try (Peer peer = new Peer(false, h2c, false)) {
            final Echo echo = client(name).get();
            try {
                endpoint(echo, peer);
                invokeTwice(peer, echo, expected, preflight);
                if (name.equals("h2")) {
                    assertThat(peer.wire()).startsWith("PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n");
                }
            } finally {
                client(name).destroy(echo);
            }
        }
    }

    static Stream<Arguments> overrides() {
        return Stream.of(Arguments.of(FORCE, "2", true), Arguments.of(FORCE, "2", false));
    }

    @ParameterizedTest
    @MethodSource("overrides")
    void legacyOverridesUseBodylessPreflight(String key, Object value, boolean h2c) throws Exception {
        try (Peer peer = new Peer(false, h2c, false)) {
            final Instance<Echo> selected = client((key.equals(FORCE) ? "force" : "enable") + (h2c ? "Upgrade" : "Refusal"));
            final Echo echo = selected.get();
            try {
                endpoint(echo, peer);
                ((BindingProvider) echo).getRequestContext().put(key, value);
                invokeTwice(peer, echo, h2c ? "HTTP_2" : "HTTP_1_1", true);
            } finally {
                selected.destroy(echo);
            }
        }
    }

    @Test
    @Disabled("https://github.com/quarkiverse/quarkus-cxf/issues/2438 - version overrides are ignored after pooling")
    void legacyOverrideAfterPooledRequest() throws Exception {
        try (Peer peer = new Peer(false, true, false)) {
            final Echo echo = legacyDefault.get();
            try {
                endpoint(echo, peer);
                assertThat(echo.echo(PAYLOAD)).isEqualTo("HTTP_1_1:100000");
                ((BindingProvider) echo).getRequestContext().put(FORCE, "2");
                assertThat(echo.echo(PAYLOAD)).isEqualTo("HTTP_2:100000");
                ((BindingProvider) echo).getRequestContext().remove(FORCE);
                assertThat(echo.echo(PAYLOAD)).isEqualTo("HTTP_1_1:100000");
                final List<Request> soap = peer.requests.stream().filter(r -> r.method().equals("POST")).toList();
                assertThat(soap).extracting(Request::version).containsExactly("HTTP_1_1", "HTTP_2", "HTTP_1_1");
                assertThat(soap.get(0).connection()).isEqualTo(soap.get(2).connection());
                assertThat(peer.wire().split("OPTIONS ", -1).length - 1).isEqualTo(1);
            } finally {
                legacyDefault.destroy(echo);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { FORCE, "policy" })
    void explicitListRejectsOverridesBeforeDispatch(String override) throws Exception {
        try (Peer peer = new Peer(false, true, false)) {
            final Instance<Echo> selected = client(override.equals(FORCE) ? "overrideForce" : "overridePolicy");
            final Echo echo = selected.get();
            try {
                endpoint(echo, peer);
                if (override.equals("policy")) {
                    final var policy = ((HTTPConduit) ClientProxy.getClient(echo).getConduit()).getClient();
                    policy.setVersion("1.0");
                    policy.setAllowChunking(false);
                } else {
                    ((BindingProvider) echo).getRequestContext().put(override, "2");
                }
                assertThatThrownBy(() -> echo.echo(PAYLOAD)).hasStackTraceContaining("vertx.http-versions");
                assertThat(peer.requests).isEmpty();
            } finally {
                selected.destroy(echo);
            }
        }
    }

    @Test
    void urlConnectionRejectsExplicitList() {
        assertThatThrownBy(() -> {
            final Echo echo = urlConnection.get();
            try {
                // Client creation itself must reject the unsupported conduit.
            } finally {
                urlConnection.destroy(echo);
            }
        }).hasStackTraceContaining("vertx.http-versions").hasStackTraceContaining("URLConnection");
    }

    @Test
    void unsupportedListIsRejectedOnClientCreation() {
        assertThatThrownBy(() -> {
            final Echo echo = unsupported.get();
            unsupported.destroy(echo);
        }).hasStackTraceContaining("Unsupported quarkus.cxf.client.unsupported.vertx.http-versions = 1.1,2;"
                + " supported values are '1.0', '1.1', '2', '2,1.1', '3', '3,2,1.1'");
    }

    @Test
    void priorKnowledgeDoesNotFallBackToHttp1() throws Exception {
        try (Peer peer = new Peer(false, false, false)) {
            final Echo echo = h2.get();
            try {
                endpoint(echo, peer);
                assertThatThrownBy(() -> echo.echo(PAYLOAD)).isInstanceOf(Exception.class);
                assertThat(peer.requests).noneMatch(request -> request.method().equals("POST"));
                assertThat(peer.wire()).startsWith("PRI * HTTP/2.0");
            } finally {
                h2.destroy(echo);
            }
        }
    }

    static Stream<Arguments> https() {
        return Stream.of(Arguments.of("tlsPrefer", true, true, "HTTP_2"),
                Arguments.of("tlsPrefer", false, true, "HTTP_1_1"),
                Arguments.of("tlsPrefer", false, false, "HTTP_1_1"),
                Arguments.of("tlsH2", true, true, "HTTP_2"),
                Arguments.of("tlsLegacy2", true, true, "HTTP_2"),
                Arguments.of("tlsH10", false, false, "HTTP_1_0"),
                Arguments.of("tlsH11", false, false, "HTTP_1_1"),
                /* The client does not offer h2, hence HTTP/1.1 even though the service would prefer HTTP/2 */
                Arguments.of("tlsH11", true, true, "HTTP_1_1"));
    }

    @ParameterizedTest(name = "{0}, h2={1}, ALPN={2}")
    @MethodSource("https")
    void httpsSelection(String name, boolean h2, boolean alpn, String expected) throws Exception {
        try (Peer peer = new Peer(true, h2, alpn)) {
            final Echo echo = client(name).get();
            try {
                endpoint(echo, peer);
                invokeTwice(peer, echo, expected, false);
                if (!name.equals("tlsLegacy2")) {
                    assertThat(peer.advertisedProtocols()).containsExactlyElementsOf(switch (name) {
                        case "tlsH2" -> List.of("h2");
                        /* No ALPN without HTTP/2 */
                        case "tlsH10", "tlsH11" -> List.of();
                        default -> List.of("h2", "http/1.1");
                    });
                }
            } finally {
                client(name).destroy(echo);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void httpsHttp2OnlyRejectsHttp1(boolean alpn) throws Exception {
        try (Peer peer = new Peer(true, false, alpn)) {
            final Echo echo = tlsH2.get();
            try {
                endpoint(echo, peer);
                assertThatThrownBy(() -> echo.echo(PAYLOAD)).isInstanceOf(Exception.class);
                assertThat(peer.requests).isEmpty();
            } finally {
                tlsH2.destroy(echo);
            }
        }
    }

    @Test
    void certificateFailureDoesNotSendSoap() throws Exception {
        try (Peer peer = new Peer(true, true, true)) {
            final Echo echo = untrusted.get();
            try {
                endpoint(echo, peer);
                assertThatThrownBy(() -> echo.echo(PAYLOAD)).hasStackTraceContaining("SSLHandshakeException");
                assertThat(peer.requests).isEmpty();
            } finally {
                untrusted.destroy(echo);
            }
        }
    }

    @Inject
    TlsConfigurationRegistry tlsRegistry;

    @Inject
    Event<CertificateUpdatedEvent> certificateUpdates;

    @Test
    void tlsUpdateRecreationAndNamedClientIndependence() throws Exception {
        try (Peer first = new Peer(true, true, true); Peer second = new Peer(true, false, false)) {
            final Echo echo = lifecycle.get();
            final Echo http1 = h11.get();
            try {
                endpoint(echo, first);
                assertThat(echo.echo(PAYLOAD)).isEqualTo("HTTP_2:100000");
                endpoint(http1, first);
                assertThat(http1.echo(PAYLOAD)).isEqualTo("HTTP_1_1:100000");
                final var tls = tlsRegistry.get("ordered").orElseThrow();
                assertThat(tls.reload()).isTrue();
                certificateUpdates.fire(new CertificateUpdatedEvent("ordered", tls));
                endpoint(echo, second);
                assertThat(echo.echo(PAYLOAD)).isEqualTo("HTTP_1_1:100000");
                assertThat(second.advertisedProtocols()).containsExactly("h2", "http/1.1");
            } finally {
                lifecycle.destroy(echo);
                h11.destroy(http1);
            }
            final Echo recreated = lifecycle.get();
            try {
                endpoint(recreated, first);
                assertThat(recreated.echo(PAYLOAD)).isEqualTo("HTTP_2:100000");
            } finally {
                lifecycle.destroy(recreated);
            }
        }
    }

    @Test
    void customizerCannotBroadenList() throws Exception {
        try (Peer peer = new Peer(false, true, false)) {
            new HttpClientPool.HttpClientPoolRecorder().addHttpClientCustomizer(new RuntimeValue<>((info, options) -> {
                if (info.getConfigKey().equals("customDirect")) {
                    options.setProtocolVersion(HttpVersion.HTTP_1_1);
                    options.setMaxHeaderSize(32_768);
                    // HTTP/2 has a separate header-list limit; maxHeaderSize only configures the HTTP/1 parser.
                    options.getInitialSettings().setMaxHeaderListSize(32_768);
                }
            }));
            final Echo echo = customDirect.get();
            try {
                ((BindingProvider) echo).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                        peer.url().replace("/echo", "/large-header"));
                assertThat(echo.echo(PAYLOAD)).isEqualTo("HTTP_2:100000");
                assertThat(peer.requests).singleElement()
                        .satisfies(request -> assertThat(request.version()).isEqualTo("HTTP_2"));
                assertThat(peer.wire()).startsWith("PRI * HTTP/2.0");
            } finally {
                customDirect.destroy(echo);
            }
        }
    }

    @Test
    void customizerProxyAndAuthenticationPreserveExplicitVersions() throws Exception {
        try (Peer proxy = new Peer(false, false, false)) {
            new HttpClientPool.HttpClientPoolRecorder().addHttpClientCustomizer(new RuntimeValue<>((info, options) -> {
                if (info.getConfigKey().equals("customized")) {
                    options.setProxyOptions(new ProxyOptions().setType(ProxyType.HTTP)
                            .setHost("localhost").setPort(proxy.recorder.actualPort()));
                    options.setMaxHeaderSize(32_768);
                    options.setProtocolVersion(HttpVersion.HTTP_2);
                }
            }));
            final Echo echo = customized.get();
            try {
                ((BindingProvider) echo).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                        "http://unresolvable.invalid/large-header");
                assertThat(echo.echo(PAYLOAD)).isEqualTo("HTTP_1_1:100000");
                assertThat(proxy.requests).singleElement().satisfies(request -> {
                    assertThat(request.version()).isEqualTo("HTTP_1_1");
                    assertThat(request.authorization()).isEqualTo("Basic YWxpY2U6c2VjcmV0");
                    assertThat(request.uri()).startsWith("http://unresolvable.invalid/");
                });
                assertThat(proxy.wire()).doesNotContain("OPTIONS ");
            } finally {
                customized.destroy(echo);
            }
        }
    }

    private static void endpoint(Echo echo, Peer peer) {
        ((BindingProvider) echo).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, peer.url());
    }

    private static void invokeTwice(Peer peer, Echo echo, String expected, boolean preflight) {
        for (int i = 1; i <= 2; i++) {
            final String response = echo.echo(PAYLOAD);
            assertThat(response).isEqualTo(expected + ":" + PAYLOAD.length());
            final List<Request> soap = peer.requests.stream().filter(r -> r.method().equals("POST")).toList();
            assertThat(soap).hasSize(i).allSatisfy(request -> {
                assertThat(request.version()).isEqualTo(expected);
                assertThat(request.body()).contains(PAYLOAD);
            });
            if (i == 2 && !expected.equals("HTTP_1_0")) {
                assertThat(soap.get(0).connection()).isEqualTo(soap.get(1).connection());
            }
            if (!peer.tls) {
                final String wire = peer.wire();
                assertThat(wire.split("OPTIONS ", -1).length - 1).isEqualTo(preflight ? 1 : 0);
                if (preflight) {
                    assertThat(wire).startsWith("OPTIONS ");
                    final int end = wire.indexOf("\r\n\r\n");
                    assertThat(end).isPositive();
                    assertThat(wire.substring(0, end)).containsIgnoringCase("upgrade: h2c");
                    // The next bytes must start HTTP/2 or the real POST, never a preflight body.
                    assertThat(wire.substring(end + 4)).startsWith(expected.equals("HTTP_2") ? "PRI * HTTP/2.0" : "POST ");
                }
            } else {
                assertThat(peer.requests).noneMatch(r -> r.method().equals("OPTIONS"));
            }
        }
    }

    @WebService(targetNamespace = "urn:ordered")
    public interface Echo {
        String echo(String body);
    }

    record Request(String method, String version, String body, String connection, String authorization, String uri) {
    }

    /** A real Vert.x SOAP peer behind a byte-recording TCP relay. */
    static final class Peer implements AutoCloseable {
        final Vertx vertx = Vertx.vertx();
        final List<Request> requests = new CopyOnWriteArrayList<>();
        final List<Buffer> received = new CopyOnWriteArrayList<>();
        final boolean tls;
        final HttpServer server;
        final NetClient forwarder;
        final NetServer recorder;

        Peer(boolean tls, boolean h2, boolean alpn) throws Exception {
            this.tls = tls;
            final HttpServerOptions options = new HttpServerOptions().setHost("localhost").setPort(0)
                    .setHttp2ClearTextEnabled(h2);
            if (tls) {
                options.setSsl(true).setUseAlpn(alpn)
                        .setKeyCertOptions(new PfxOptions().setPath("target/classes/ordered-localhost-keystore.p12")
                                .setPassword("secret"))
                        .setAlpnVersions(h2 ? List.of(HttpVersion.HTTP_2, HttpVersion.HTTP_1_1)
                                : List.of(HttpVersion.HTTP_1_1));
            }
            server = await(vertx.createHttpServer(options).requestHandler(request -> request.body().onSuccess(body -> {
                requests.add(new Request(request.method().name(), request.version().name(), body.toString(),
                        request.remoteAddress().toString(), request.getHeader("Authorization"), request.uri()));
                if (request.method().name().equals("OPTIONS")) {
                    request.response().setStatusCode(200).end("preflight is not a SOAP response");
                } else {
                    if (request.path().equals("/large-header")) {
                        request.response().putHeader("X-Large", "a".repeat(16_384));
                    }
                    final String response = request.version().name() + ":" + PAYLOAD.length();
                    request.response().putHeader("Content-Type", "text/xml; charset=UTF-8").end(
                            "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"><s:Body>"
                                    + "<echoResponse xmlns=\"urn:ordered\"><return xmlns=\"\">" + response
                                    + "</return></echoResponse></s:Body></s:Envelope>");
                }
            })).listen());
            forwarder = vertx.createNetClient();
            recorder = await(vertx.createNetServer().connectHandler(incoming -> {
                incoming.pause();
                forwarder.connect(server.actualPort(), "localhost").onSuccess(outgoing -> {
                    incoming.handler(bytes -> {
                        received.add(bytes.copy());
                        outgoing.write(bytes);
                    });
                    outgoing.handler(incoming::write);
                    incoming.closeHandler(ignored -> outgoing.close());
                    outgoing.closeHandler(ignored -> incoming.close());
                    incoming.resume();
                }).onFailure(ignored -> incoming.close());
            }).listen(0, "localhost"));
        }

        String url() {
            return (tls ? "https" : "http") + "://localhost:" + recorder.actualPort() + "/echo";
        }

        String wire() {
            final Buffer all = Buffer.buffer();
            received.forEach(all::appendBuffer);
            return all.toString(StandardCharsets.ISO_8859_1);
        }

        /** Decode the ALPN extension in the first TLS ClientHello, independently of server preference. */
        List<String> advertisedProtocols() {
            final Buffer bytes = Buffer.buffer();
            received.forEach(bytes::appendBuffer);
            final ByteBuffer hello = ByteBuffer.wrap(bytes.getBytes());
            assertThat(Byte.toUnsignedInt(hello.get())).isEqualTo(22); // TLS handshake record
            hello.position(5);
            assertThat(Byte.toUnsignedInt(hello.get())).isEqualTo(1); // ClientHello
            hello.position(9 + 2 + 32);
            skip(hello, Byte.toUnsignedInt(hello.get())); // session ID
            skip(hello, Short.toUnsignedInt(hello.getShort())); // cipher suites
            skip(hello, Byte.toUnsignedInt(hello.get())); // compression methods
            final int end = Short.toUnsignedInt(hello.getShort()) + hello.position();
            while (hello.position() < end) {
                final int type = Short.toUnsignedInt(hello.getShort());
                final int length = Short.toUnsignedInt(hello.getShort());
                if (type == 16) {
                    final int protocolsEnd = Short.toUnsignedInt(hello.getShort()) + hello.position();
                    final List<String> protocols = new ArrayList<>();
                    while (hello.position() < protocolsEnd) {
                        final byte[] protocol = new byte[Byte.toUnsignedInt(hello.get())];
                        hello.get(protocol);
                        protocols.add(new String(protocol, StandardCharsets.US_ASCII));
                    }
                    return List.copyOf(protocols);
                }
                skip(hello, length);
            }
            return List.of();
        }

        private static void skip(ByteBuffer buffer, int length) {
            buffer.position(buffer.position() + length);
        }

        @Override
        public void close() throws Exception {
            await(vertx.close());
        }

        private static <T> T await(Future<T> future) throws Exception {
            return future.toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
        }
    }
}
