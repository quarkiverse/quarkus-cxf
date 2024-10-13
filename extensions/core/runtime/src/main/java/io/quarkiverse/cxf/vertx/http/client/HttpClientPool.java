package io.quarkiverse.cxf.vertx.http.client;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.transport.http.HTTPConduit;

import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.runtime.config.TlsConfigUtils;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpVersion;

@ApplicationScoped
public class HttpClientPool {
    private final Map<ClientSpec, HttpClient> clients = new ConcurrentHashMap<>();
    /** Not a concurrent map, has to be synchronized */
    private final Map<String, Map.Entry<ClientSpec, HttpClient>> clientsByTlsConfigurationName = new HashMap<>();
    private final Vertx vertx;

    HttpClientPool() {
        this(null);
    }

    @Inject
    public HttpClientPool(Vertx vertx) {
        super();
        this.vertx = vertx;
    }

    public HttpClient getClient(ClientSpec spec) {
        return clients.computeIfAbsent(spec, v -> {
            synchronized (clientsByTlsConfigurationName) {
                final HttpClientOptions opts = newHttpClientOptions(spec);
                final HttpClient client = vertx.createHttpClient(opts);
                if (spec.tlsConfigurationName != null) {
                    clientsByTlsConfigurationName.put(spec.tlsConfigurationName,
                            new AbstractMap.SimpleImmutableEntry<>(spec, client));
                }
                return client;
            }
        });
    }

    static HttpClientOptions newHttpClientOptions(ClientSpec spec) {
        final HttpClientOptions opts = new HttpClientOptions()
                .setProtocolVersion(spec.getVersion());
        if (spec.isSsl()) {
            TlsConfigUtils.configure(opts, spec.tlsConfiguration);
        }
        return opts;
    }

    public static class ClientSpec {
        protected static final Logger LOG = LogUtils.getL7dLogger(HTTPConduit.class);
        private final HttpVersion httpVersion;
        private final TlsConfiguration tlsConfiguration;
        private final String tlsConfigurationName;

        private final int hashCode;

        public ClientSpec(
                HttpVersion version,
                String tlsConfigurationName,
                TlsConfiguration tlsConfiguration) {

            this.httpVersion = version;
            this.tlsConfigurationName = tlsConfigurationName;
            this.tlsConfiguration = tlsConfiguration;
            int h = 31 + httpVersion.hashCode();
            if (this.tlsConfiguration != null) {
                h = 31 * h + this.tlsConfiguration.hashCode();
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
