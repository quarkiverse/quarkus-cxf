package io.quarkiverse.cxf.vertx.http.client;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.transport.http.HTTPConduit;

import io.quarkus.tls.CertificateUpdatedEvent;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.runtime.config.TlsConfigUtils;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpVersion;

/**
 * A pool of HTTP clients so that we do not have to reconnect on every request.
 */
@ApplicationScoped
public class HttpClientPool {
    private final Map<ClientSpec, HttpClient> clients = new ConcurrentHashMap<>();
    private final Vertx vertx;

    HttpClientPool() {
        this(null);
    }

    @Inject
    public HttpClientPool(Vertx vertx) {
        super();
        this.vertx = vertx;
    }

    /**
     * If this method returns a client that is concurrently being removed by
     * {@link #onCertificateUpdate(CertificateUpdatedEvent)} then the client may still work for one request, but will be
     * re-created on the subsequent request.
     *
     * @param spec the caching key
     * @return a possibly pooled client
     */
    public HttpClient getClient(ClientSpec spec) {
        return clients.computeIfAbsent(spec, v -> {
            final HttpClientOptions opts = new HttpClientOptions()
                    .setProtocolVersion(spec.getVersion());
            if (spec.isSsl()) {
                TlsConfigUtils.configure(opts, spec.tlsConfiguration);
            }
            return vertx.createHttpClient(opts);
        });
    }

    /**
     * Called upon certificate reload. Clients having the given {@link ClientSpec#tlsConfigurationName} will be
     * removed from this pool, so that they are created anew via {@link #getClient(ClientSpec)} on the next request.
     *
     * @param event the update event
     */
    public void onCertificateUpdate(@Observes CertificateUpdatedEvent event) {
        final String tlsConfigName = event.name();
        if (tlsConfigName != null) {
            final Iterator<ClientSpec> it = clients.keySet().iterator();
            while (it.hasNext()) {
                final ClientSpec spec = it.next();
                if (tlsConfigName.equals(spec.tlsConfigurationName)) {
                    it.remove();
                }
            }
        }
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
