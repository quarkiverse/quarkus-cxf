package io.quarkiverse.cxf.quarkus.vertx.http.client.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkiverse.cxf.vertx.http.client.QuicSupport;
import io.quarkus.maven.dependency.ArtifactCoords;

class QuicSupportTest {
    private static final ArtifactCoords VERTX_CORE = ArtifactCoords.jar("io.vertx", "vertx-core", "5.2.3");
    /* The artifact without classifier contains no native library */
    private static final ArtifactCoords QUIC_NO_CLASSIFIER = ArtifactCoords.jar("io.netty", "netty-codec-native-quic",
            "4.2.20.Final");
    private static final ArtifactCoords QUIC_LINUX = ArtifactCoords.jar("io.netty", "netty-codec-native-quic", "linux-x86_64",
            "4.2.20.Final");

    @Test
    void quarkusHttp3Missing() {
        assertThat(VertxWebClientProcessor.findQuicSupport(false, List.of(VERTX_CORE)))
                .isEqualTo(QuicSupport.QUARKUS_HTTP3_MISSING);
        /* The native library alone works only in JVM mode, so we still require quarkus-http3 */
        assertThat(VertxWebClientProcessor.findQuicSupport(false, List.of(VERTX_CORE, QUIC_LINUX)))
                .isEqualTo(QuicSupport.QUARKUS_HTTP3_MISSING);
    }

    @Test
    void nativeLibraryMissing() {
        assertThat(VertxWebClientProcessor.findQuicSupport(true, List.of(VERTX_CORE)))
                .isEqualTo(QuicSupport.NATIVE_LIBRARY_MISSING);
        assertThat(VertxWebClientProcessor.findQuicSupport(true, List.of(VERTX_CORE, QUIC_NO_CLASSIFIER)))
                .isEqualTo(QuicSupport.NATIVE_LIBRARY_MISSING);
    }

    @Test
    void available() {
        assertThat(VertxWebClientProcessor.findQuicSupport(true, List.of(VERTX_CORE, QUIC_NO_CLASSIFIER, QUIC_LINUX)))
                .isEqualTo(QuicSupport.AVAILABLE);
    }
}
