package io.quarkiverse.cxf.vertx.http.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.vertx.core.http.HttpClientConfig;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.ProxyOptions;
import io.vertx.core.net.ProxyType;

/**
 * How {@link HttpClientPool} configures Vert.x {@link io.vertx.core.http.HttpClient} for clients having
 * {@link HttpVersion#HTTP_3} among their HTTP versions.
 */
class Http3ClientConfigTest {

    static Stream<List<HttpVersion>> versions() {
        return Stream.of(
                List.of(HttpVersion.HTTP_3),
                List.of(HttpVersion.HTTP_3, HttpVersion.HTTP_2, HttpVersion.HTTP_1_1),
                List.of(HttpVersion.HTTP_2, HttpVersion.HTTP_3, HttpVersion.HTTP_1_1),
                List.of(HttpVersion.HTTP_2, HttpVersion.HTTP_1_1, HttpVersion.HTTP_3),
                List.of(HttpVersion.HTTP_1_1, HttpVersion.HTTP_3),
                List.of(HttpVersion.HTTP_3, HttpVersion.HTTP_1_0));
    }

    /** Mimics what {@link HttpClientPool#getClient} does before calling {@link HttpClientPool#toHttp3Config} */
    private static HttpClientOptions options(List<HttpVersion> versions) {
        final HttpClientOptions result = new HttpClientOptions();
        final List<HttpVersion> tcpVersions = HttpClientPool.tcpVersions(versions);
        if (!tcpVersions.isEmpty()) {
            result.setProtocolVersion(tcpVersions.get(0)).setAlpnVersions(tcpVersions).setUseAlpn(true);
        }
        return result;
    }

    @ParameterizedTest
    @MethodSource("versions")
    void versionsAndAlternativeServices(List<HttpVersion> versions) {
        final HttpClientConfig config = HttpClientPool.toHttp3Config(options(versions), versions, 60, 30_000L);
        /* The configured order is passed to Vert.x as is */
        assertThat(config.getVersions()).containsExactlyElementsOf(versions);
        /*
         * A sole HTTP/3 means HTTP/3 right away without fallback.
         * Otherwise Vert.x has to be told to follow Alt-Svc advertisements, or else HTTP/3 would never be used.
         */
        assertThat(config.getFollowAlternativeServices()).isEqualTo(versions.size() > 1);
    }

    @Test
    void tcpVersions() {
        assertThat(HttpClientPool.tcpVersions(List.of())).isEmpty();
        assertThat(HttpClientPool.tcpVersions(List.of(HttpVersion.HTTP_3))).isEmpty();
        assertThat(HttpClientPool.tcpVersions(List.of(HttpVersion.HTTP_2, HttpVersion.HTTP_3, HttpVersion.HTTP_1_1)))
                .containsExactly(HttpVersion.HTTP_2, HttpVersion.HTTP_1_1);
        assertThat(HttpClientPool.tcpVersions(List.of(HttpVersion.HTTP_1_1, HttpVersion.HTTP_2)))
                .containsExactly(HttpVersion.HTTP_1_1, HttpVersion.HTTP_2);
    }

    /**
     * {@code vertx.http3-keep-alive-timeout} and the HTTP version independent connection timeout have to reach the
     * HTTP/3 and QUIC specific configuration, without leaking to the keep alive timeouts of the TCP based HTTP versions
     */
    @ParameterizedTest
    @MethodSource("versions")
    void timeouts(List<HttpVersion> versions) {
        final HttpClientConfig config = HttpClientPool.toHttp3Config(options(versions), versions, 17, 2_345L);
        assertThat(config.getHttp3Config().getKeepAliveTimeout()).isEqualTo(Duration.ofSeconds(17));
        assertThat(config.getQuicConfig().getConnectTimeout()).isEqualTo(Duration.ofMillis(2_345L));
        assertThat(config.getHttp1Config().getKeepAliveTimeout())
                .isEqualTo(Duration.ofSeconds(HttpClientOptions.DEFAULT_KEEP_ALIVE_TIMEOUT));
        assertThat(config.getHttp2Config().getKeepAliveTimeout())
                .isEqualTo(Duration.ofSeconds(HttpClientOptions.DEFAULT_HTTP2_KEEP_ALIVE_TIMEOUT));

        /* 0 means no timeout */
        final HttpClientConfig noTimeouts = HttpClientPool.toHttp3Config(options(versions), versions, 0, 0L);
        assertThat(noTimeouts.getHttp3Config().getKeepAliveTimeout()).isEqualTo(Duration.ZERO);
        assertThat(noTimeouts.getQuicConfig().getConnectTimeout()).isEqualTo(Duration.ZERO);
    }

    /**
     * Whatever {@code HttpClientCustomizerBuildItem}s, such as the one of {@code quarkus-cxf-rt-features-metrics}, set
     * on {@link HttpClientOptions} has to be taken over
     */
    @ParameterizedTest
    @MethodSource("versions")
    void httpClientOptionsTakenOver(List<HttpVersion> versions) {
        final HttpClientOptions options = options(versions)
                .setMetricsName("cxf|myClient")
                .setKeepAliveTimeout(23)
                .setHttp2KeepAliveTimeout(29)
                .setMaxHeaderSize(32_768)
                .setProxyOptions(new ProxyOptions().setType(ProxyType.HTTP).setHost("proxy.example.org").setPort(3128))
                .addNonProxyHost("localhost");
        final HttpClientConfig config = HttpClientPool.toHttp3Config(options, versions, 60, 30_000L);
        assertThat(config.getObservabilityConfig().getMetricsName()).isEqualTo("cxf|myClient");
        assertThat(config.getHttp1Config().getKeepAliveTimeout()).isEqualTo(Duration.ofSeconds(23));
        assertThat(config.getHttp1Config().getMaxHeaderSize()).isEqualTo(32_768);
        assertThat(config.getHttp2Config().getKeepAliveTimeout()).isEqualTo(Duration.ofSeconds(29));
        /* ... and the HTTP/1.x and HTTP/2 keep alive timeouts must not leak to HTTP/3 */
        assertThat(config.getHttp3Config().getKeepAliveTimeout()).isEqualTo(Duration.ofSeconds(60));
        assertThat(config.getTcpConfig().getProxyOptions().getHost()).isEqualTo("proxy.example.org");
        assertThat(config.getTcpConfig().getNonProxyHosts()).containsExactly("localhost");
    }

    /** A customizer must not be able to change the versions configured by the user */
    @Test
    void customizedProtocolVersionIgnored() {
        final List<HttpVersion> versions = List.of(HttpVersion.HTTP_3);
        final HttpClientOptions options = options(versions).setProtocolVersion(HttpVersion.HTTP_2);
        assertThat(HttpClientPool.toHttp3Config(options, versions, 60, 30_000L).getVersions())
                .containsExactly(HttpVersion.HTTP_3);
    }
}
