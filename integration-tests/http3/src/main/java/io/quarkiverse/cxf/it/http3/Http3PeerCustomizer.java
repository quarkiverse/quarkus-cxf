package io.quarkiverse.cxf.it.http3;

import jakarta.inject.Singleton;

import io.quarkus.vertx.http.HttpServerConfigCustomizer;
import io.vertx.core.http.HttpServerConfig;
import io.vertx.core.net.ServerSSLOptions;

/**
 * A customizer limiting the number of concurrent requests which the HTTP server of this application accepts over a
 * single HTTP/3 connection.
 * <p>
 * The limit holds for the whole HTTP server, hence for both {@link Http3Peer} and the services exposed by Quarkus CXF,
 * such as {@link ProtocolEchoImpl}.
 * <p>
 * Thanks to that limit, a SOAP client sending more concurrent requests has to open more HTTP/3 connections, if its
 * {@code quarkus.cxf.client."client-name".vertx.connection-pool.http3-max-size} allows that.
 * Otherwise, some of its requests have to wait for the others to complete.
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
