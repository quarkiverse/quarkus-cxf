package io.quarkiverse.cxf.vertx.http.client;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.transport.http.HTTPConduit;

import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.runtime.config.TlsConfigUtils;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpVersion;

public class HttpClientPool {
    private final Map<ClientSpec, HttpClient> clients = new ConcurrentHashMap<>();
    private final Vertx vertx;

    public HttpClientPool(Vertx vertx) {
        super();
        this.vertx = vertx;
    }

    public HttpClient getClient(ClientSpec spec) {
        return clients.computeIfAbsent(spec, v -> {
            final HttpClientOptions opts = new HttpClientOptions()
                    .setProtocolVersion(spec.getVersion());
            if (spec.isSsl()) {
                TlsConfigUtils.configure(opts, spec.tlsConfiguration);
                /* We trust all here, but pass a HostnameVerifier via X509TrustManagerWrapper */
                // Do not support HostNameVerifier in VertxClient tlsRegistry
                // opts.setTrustAll(true);
            }
            return vertx.createHttpClient(opts);
        });
    }

    public static class ClientSpec {
        protected static final Logger LOG = LogUtils.getL7dLogger(HTTPConduit.class);
        private final HttpVersion httpVersion;
        private final TlsConfiguration tlsConfiguration;

        private final int hashCode;

        public ClientSpec(
                HttpVersion version,
                TlsConfiguration tlsConfiguration) {

            this.httpVersion = version;
            this.tlsConfiguration = tlsConfiguration;
            int h = 31 + httpVersion.hashCode();
            if (this.tlsConfiguration != null) {
                h = 31 * h + tlsConfiguration.hashCode();
            }

            this.hashCode = h;
        }

        public boolean isSsl() {
            return tlsConfiguration != null;
        }

        public HttpVersion getVersion() {
            return httpVersion;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ClientSpec other = (ClientSpec) obj;
            return httpVersion == other.httpVersion
                    && Objects.equals(tlsConfiguration, other.tlsConfiguration);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

    }

}
