package io.quarkiverse.cxf.vertx.http.client;

/**
 * The availability of QUIC, the transport protocol of HTTP/3, in the current application, as figured out at build time.
 *
 * @since 4.0.0
 */
public enum QuicSupport {
    /** {@code io.quarkus:quarkus-http3} and a native QUIC library are available */
    AVAILABLE,
    /**
     * {@code io.quarkus:quarkus-http3} is not available.
     * Without it, {@code quarkus-vertx} removes the QUIC support from Vert.x in native mode.
     */
    QUARKUS_HTTP3_MISSING,
    /**
     * {@code io.quarkus:quarkus-http3} is available, but there is no {@code io.netty:netty-codec-native-quic} artifact
     * containing a native QUIC library among the dependencies of the application
     */
    NATIVE_LIBRARY_MISSING
}
