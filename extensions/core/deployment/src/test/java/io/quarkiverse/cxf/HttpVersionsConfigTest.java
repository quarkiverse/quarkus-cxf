package io.quarkiverse.cxf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.cxf.transports.http.configuration.ConnectionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkiverse.cxf.vertx.http.client.QuicSupport;
import io.quarkus.proxy.ProxyConfiguration;
import io.quarkus.proxy.ProxyType;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.PoolOptions;

class HttpVersionsConfigTest {
    private static final String CLIENT = "ordered.client";
    private static final String PREFIX = "quarkus.cxf.client.\"" + CLIENT + "\"";
    private static final String PROPERTY = PREFIX + ".vertx.http-versions";
    private static final Supplier<String> PROPERTY_SUPPLIER = () -> PROPERTY;
    /** What {@link ProxyConfigurationUtils#proxiedHosts(ProxyConfiguration)} returns when there is no proxy */
    private static final Predicate<String> NO_PROXY = ProxyConfigurationUtils.proxiedHosts(null);

    static Stream<List<String>> versions() {
        return Stream.of(List.of("1.0"), List.of("1.1"), List.of("2"),
                List.of("2", "1.1"), List.of("1.1", "2"), List.of("2", "1.0"),
                List.of("2", "1.1", "1.0"), List.of("2", "1.0", "1.1"),
                List.of("1.0", "2", "1.1"), List.of("1.0", "1.1", "2"),
                List.of("1.1", "1.0", "2"), List.of("1.1", "2", "1.0"),
                List.of("3"), List.of("3", "2", "1.1"), List.of("2", "3", "1.1"), List.of("2", "1.1", "3"),
                List.of("3", "1.1"), List.of("1.0", "3"), List.of("3", "2", "1.1", "1.0"));
    }

    @ParameterizedTest
    @MethodSource("versions")
    void orderedImmutableVersions(List<String> values) {
        final List<HttpVersion> actual = parse(Map.of(PROPERTY, String.join(",", values)));
        assertThat(actual).containsExactlyElementsOf(values.stream().map(value -> switch (value) {
            case "1.0" -> HttpVersion.HTTP_1_0;
            case "1.1" -> HttpVersion.HTTP_1_1;
            case "2" -> HttpVersion.HTTP_2;
            case "3" -> HttpVersion.HTTP_3;
            default -> throw new IllegalArgumentException(value);
        }).toList());
        assertThatThrownBy(() -> actual.add(HttpVersion.HTTP_1_1)).isInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = { "4", "auto", "HTTP_2", "2.0", "3.0", "h3", "bogus", "2,4", "2,2", "1.0,1.1,1.0", "1.1,1.1",
            "3,3", "3,2,3" })
    void invalidValuesIdentifyClientAndCanonicalProperty(String value) {
        assertThatThrownBy(() -> parse(Map.of(PROPERTY, value)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(PROPERTY)
                .hasMessageContaining(value.substring(value.lastIndexOf(',') + 1));
    }

    @ParameterizedTest
    @ValueSource(strings = { "1.0", "1.1", "2" })
    void legacyConflict(String legacy) {
        assertThatThrownBy(() -> parse(Map.of(PROPERTY, "2,1.1", PREFIX + ".version", legacy)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(PROPERTY).hasMessageContaining(PREFIX + ".version = " + legacy);
        assertThatThrownBy(() -> parse(Map.of(PROPERTY, "3", PREFIX + ".version", legacy)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(PROPERTY).hasMessageContaining(PREFIX + ".version = " + legacy);
    }

    @ParameterizedTest
    @ValueSource(strings = { "3", "3,2,1.1", "2,3,1.1", "2,1.1,3", "1.1,3" })
    void http3RejectsPlainHttp(String versions) {
        final List<HttpVersion> parsed = parse(Map.of(PROPERTY, versions));
        assertThatThrownBy(() -> CXFClientInfo.validateHttp3Destination(PROPERTY_SUPPLIER, parsed, null, NO_PROXY,
                URI.create("http://user:secret@example.org:8080/service?token=secret")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(PROPERTY + " = " + versions + " cannot be used with http://example.org:8080,")
                .hasMessageContaining("HTTP/3 requires an https: URL")
                /* No credentials or query parameters in diagnostics */
                .hasMessageNotContaining("secret");

        assertThatCode(() -> CXFClientInfo.validateHttp3Destination(PROPERTY_SUPPLIER, parsed, null, NO_PROXY,
                URI.create("https://example.org:8443/service")))
                .doesNotThrowAnyException();
        /* Nothing to validate for WSDLs loaded from class path or file system */
        assertThatCode(() -> CXFClientInfo.validateHttp3Destination(PROPERTY_SUPPLIER, parsed, null, NO_PROXY, null))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = { "3", "3,2,1.1", "2,3,1.1", "2,1.1,3", "1.1,3" })
    void http3RejectsProxiedDestination(String versions) {
        final List<HttpVersion> parsed = parse(Map.of(PROPERTY, versions));
        final ProxyConfiguration proxy = new TestProxyConfiguration("proxy.example.org", 3128,
                List.of("localhost", "*.internal.example.org"));
        final Predicate<String> proxiedHosts = ProxyConfigurationUtils.proxiedHosts(proxy);
        assertThatThrownBy(() -> CXFClientInfo.validateHttp3Destination(PROPERTY_SUPPLIER, parsed, proxy, proxiedHosts,
                URI.create("https://user:secret@service.example.org/service?token=secret")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(PROPERTY + " = " + versions + " cannot be used with https://service.example.org,")
                .hasMessageContaining("through the proxy proxy.example.org:3128")
                .hasMessageContaining("HTTP/3 through a proxy is not supported")
                .hasMessageContaining(PREFIX + ".proxy-configuration-name to none")
                .hasMessageNotContaining("secret");

        /* Hosts exempted from proxying may use HTTP/3 */
        for (String nonProxied : List.of("https://localhost:8443/service", "https://service.internal.example.org/service")) {
            assertThatCode(
                    () -> CXFClientInfo.validateHttp3Destination(PROPERTY_SUPPLIER, parsed, proxy, proxiedHosts,
                            URI.create(nonProxied)))
                    .doesNotThrowAnyException();
        }
        /* quarkus.cxf.client."client-name".proxy-configuration-name = none */
        final ProxyConfiguration none = new TestProxyConfiguration("none", 0, null);
        assertThatCode(() -> CXFClientInfo.validateHttp3Destination(PROPERTY_SUPPLIER, parsed, none,
                ProxyConfigurationUtils.proxiedHosts(none), URI.create("https://service.example.org/service")))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = { "1.0", "1.1", "2", "2,1.1", "1.1,2" })
    void listsWithoutHttp3AcceptPlainHttpAndProxy(String versions) {
        final List<HttpVersion> parsed = parse(Map.of(PROPERTY, versions));
        final ProxyConfiguration proxy = new TestProxyConfiguration("proxy.example.org", 3128, null);
        final Predicate<String> proxiedHosts = ProxyConfigurationUtils.proxiedHosts(proxy);
        for (String destination : List.of("http://service.example.org/service", "https://service.example.org/service")) {
            assertThatCode(
                    () -> CXFClientInfo.validateHttp3Destination(PROPERTY_SUPPLIER, parsed, proxy, proxiedHosts,
                            URI.create(destination)))
                    .doesNotThrowAnyException();
        }
        for (QuicSupport quicSupport : QuicSupport.values()) {
            assertThatCode(() -> CXFClientInfo.validateQuicSupport(PROPERTY_SUPPLIER, parsed, quicSupport))
                    .doesNotThrowAnyException();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "3", "3,2,1.1", "2,1.1,3" })
    void http3RequiresQuic(String versions) {
        final List<HttpVersion> parsed = parse(Map.of(PROPERTY, versions));
        assertThatThrownBy(
                () -> CXFClientInfo.validateQuicSupport(PROPERTY_SUPPLIER, parsed, QuicSupport.QUARKUS_HTTP3_MISSING))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(PROPERTY + " = " + versions + " requires QUIC")
                .hasMessageContaining("io.quarkus:quarkus-http3")
                .hasMessageContaining("quarkus.http3.enabled = false");
        assertThatThrownBy(
                () -> CXFClientInfo.validateQuicSupport(PROPERTY_SUPPLIER, parsed, QuicSupport.NATIVE_LIBRARY_MISSING))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(PROPERTY + " = " + versions + " requires QUIC")
                .hasMessageContaining("io.netty:netty-codec-native-quic");
        assertThatCode(() -> CXFClientInfo.validateQuicSupport(PROPERTY_SUPPLIER, parsed, QuicSupport.AVAILABLE))
                .doesNotThrowAnyException();
    }

    @Test
    void remoteUris() {
        assertThat(CXFClientInfo.toRemoteUri("http://example.org/service?wsdl"))
                .isEqualTo(URI.create("http://example.org/service?wsdl"));
        assertThat(CXFClientInfo.toRemoteUri("https://example.org/service?wsdl"))
                .isEqualTo(URI.create("https://example.org/service?wsdl"));
        for (String local : List.of("wsdl/service.wsdl", "/absolute/service.wsdl", "file:/absolute/service.wsdl",
                "classpath:wsdl/service.wsdl", "C:\\wsdl\\service.wsdl", "not a uri")) {
            assertThat(CXFClientInfo.toRemoteUri(local)).as(local).isNull();
        }
        assertThat(CXFClientInfo.toRemoteUri(null)).isNull();
    }

    /** Each pool size option impacts only the pool of its own HTTP version */
    @ParameterizedTest
    @CsvSource(nullValues = "-", value = {
            /* http1-max-size, http2-max-size, http3-max-size, expected HTTP/1.x, expected HTTP/2, expected HTTP/3 */
            "-,-,-,5,1,1",
            "3,-,-,3,1,1",
            "-,4,-,5,4,1",
            "-,-,6,5,1,6",
            "3,4,6,3,4,6",
    })
    void connectionPoolMaxSize(String http1MaxSize, String http2MaxSize, String http3MaxSize, int expectedHttp1,
            int expectedHttp2, int expectedHttp3) {
        final SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder().withMapping(CxfConfig.class)
                .withDefaultValue(PREFIX + ".client-endpoint-url", "http://localhost/unused");
        if (http1MaxSize != null) {
            builder.withDefaultValue(PREFIX + ".vertx.connection-pool.http1-max-size", http1MaxSize);
        }
        if (http2MaxSize != null) {
            builder.withDefaultValue(PREFIX + ".vertx.connection-pool.http2-max-size", http2MaxSize);
        }
        if (http3MaxSize != null) {
            builder.withDefaultValue(PREFIX + ".vertx.connection-pool.http3-max-size", http3MaxSize);
        }
        final PoolOptions poolOptions = new PoolOptions();
        builder.build().getConfigMapping(CxfConfig.class).clients().get(CLIENT).vertx().connectionPool()
                .configure(poolOptions);
        assertThat(poolOptions.getHttp1MaxSize()).isEqualTo(expectedHttp1);
        assertThat(poolOptions.getHttp2MaxSize()).isEqualTo(expectedHttp2);
        assertThat(poolOptions.getHttp3MaxSize()).isEqualTo(expectedHttp3);
    }

    /**
     * Each keep alive timeout option impacts only the connections of its own HTTP version and it does so regardless of
     * the HTTP version preferred by the client
     */
    @ParameterizedTest
    @CsvSource(nullValues = "-", value = {
            /*
             * keep-alive-timeout, http2-keep-alive-timeout, http3-keep-alive-timeout,
             * expected HTTP/1.x, expected HTTP/2, expected HTTP/3
             */
            "-,-,-,60,60,60",
            "7,-,-,7,60,60",
            "-,8,-,60,8,60",
            "-,-,9,60,60,9",
            "7,8,9,7,8,9",
            "0,0,0,0,0,0",
    })
    void keepAliveTimeouts(String keepAliveTimeout, String http2KeepAliveTimeout, String http3KeepAliveTimeout,
            int expectedHttp1, int expectedHttp2, int expectedHttp3) {
        final SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder().withMapping(CxfConfig.class)
                .withDefaultValue(PREFIX + ".client-endpoint-url", "http://localhost/unused");
        if (keepAliveTimeout != null) {
            builder.withDefaultValue(PREFIX + ".vertx.keep-alive-timeout", keepAliveTimeout);
        }
        if (http2KeepAliveTimeout != null) {
            builder.withDefaultValue(PREFIX + ".vertx.http2-keep-alive-timeout", http2KeepAliveTimeout);
        }
        if (http3KeepAliveTimeout != null) {
            builder.withDefaultValue(PREFIX + ".vertx.http3-keep-alive-timeout", http3KeepAliveTimeout);
        }
        final CxfClientConfig.VertxConfig vertxConfig = builder.build().getConfigMapping(CxfConfig.class).clients()
                .get(CLIENT).vertx();
        /* HTTP/3 timeout cannot be set through HttpClientOptions; HttpClientPool passes it to Http3ClientConfig */
        assertThat(vertxConfig.http3KeepAliveTimeout()).isEqualTo(expectedHttp3);
        for (HttpVersion preferredVersion : List.of(HttpVersion.HTTP_1_0, HttpVersion.HTTP_1_1, HttpVersion.HTTP_2)) {
            for (ConnectionType connectionType : ConnectionType.values()) {
                final HttpClientOptions opts = new HttpClientOptions().setProtocolVersion(preferredVersion);
                vertxConfig.configure(opts, connectionType, new PoolOptions());
                assertThat(opts.getKeepAliveTimeout()).as("HTTP/1.x timeout of a client preferring %s", preferredVersion)
                        .isEqualTo(expectedHttp1);
                assertThat(opts.getHttp2KeepAliveTimeout()).as("HTTP/2 timeout of a client preferring %s", preferredVersion)
                        .isEqualTo(expectedHttp2);
            }
        }
    }

    record TestProxyConfiguration(String host, int port, List<String> nonProxyHostList) implements ProxyConfiguration {

        @Override
        public Optional<String> username() {
            return Optional.empty();
        }

        @Override
        public Optional<String> password() {
            return Optional.empty();
        }

        @Override
        public Optional<List<String>> nonProxyHosts() {
            return Optional.ofNullable(nonProxyHostList);
        }

        @Override
        public Optional<Duration> proxyConnectTimeout() {
            return Optional.empty();
        }

        @Override
        public ProxyType type() {
            return ProxyType.HTTP;
        }
    }

    @Test
    void absentAndEmptyBindingRetainLegacyMode() {
        assertThat(parse(Map.of())).isEmpty();
        assertThat(parse(Map.of(PROPERTY, ""))).isEmpty();
        for (String legacy : List.of("auto", "1.0", "1.1", "2")) {
            assertThat(parse(Map.of(PREFIX + ".version", legacy))).isEmpty();
        }
        assertThat(parse(Map.of(PROPERTY, "2", PREFIX + ".version", "auto")))
                .containsExactly(HttpVersion.HTTP_2);
    }

    @Test
    void presentEmptyListIsRejected() {
        final CxfClientConfig config = new SmallRyeConfigBuilder().withMapping(EmptyVersionsConfig.class, "empty")
                .build().getConfigMapping(EmptyVersionsConfig.class, "empty");
        assertThat(config.vertx().httpVersions()).contains(List.of());
        assertThatThrownBy(() -> CXFClientInfo.parseHttpVersions(config, CLIENT))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(PROPERTY)
                .hasMessageContaining("at least one");
    }

    public interface EmptyVersionsConfig extends CxfClientConfig {
        @Override
        EmptyVertxConfig vertx();
    }

    public interface EmptyVertxConfig extends CxfClientConfig.VertxConfig {
        @Override
        default Optional<List<String>> httpVersions() {
            return Optional.of(List.of());
        }
    }

    private static List<HttpVersion> parse(Map<String, String> properties) {
        final CxfConfig config = new SmallRyeConfigBuilder().withMapping(CxfConfig.class)
                .withDefaultValue(PREFIX + ".client-endpoint-url", "http://localhost/unused")
                .withDefaultValues(properties).build().getConfigMapping(CxfConfig.class);
        return CXFClientInfo.parseHttpVersions(config.clients().get(CLIENT), CLIENT);
    }
}
