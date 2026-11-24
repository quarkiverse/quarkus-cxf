package io.quarkiverse.cxf.it.http3;

import jakarta.inject.Singleton;

import io.quarkus.vertx.http.HttpServerConfigCustomizer;
import io.vertx.core.http.HttpServerConfig;
import io.vertx.core.net.ServerSSLOptions;

/**
 * A customizer limiting the number of concurrent requests which {@link Http3Peer} accepts over a single HTTP/3
 * connection.
 * <p>
 * Thanks to that limit, a SOAP client sending more concurrent requests has to open more HTTP/3 connections, if its
 * {@code quarkus.cxf.client."client-name".vertx.connection-pool.http3-max-size} allows that.
 */
@Singleton
public class Http3PeerCustomizer implements HttpServerConfigCustomizer {

    /** The maximum number of concurrent requests per HTTP/3 connection */
    public static final int MAX_CONCURRENT_HTTP3_REQUESTS = 2;

    @Override
    public void customizeHttpsServer(HttpServerConfig config, ServerSSLOptions sslOptions) {
        config.getQuicConfig().getTransportConfig().setInitialMaxStreamsBidi(MAX_CONCURRENT_HTTP3_REQUESTS);
    }
}
