package io.quarkiverse.cxf.vertx.http.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import io.quarkiverse.cxf.CXFClientInfo;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
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
    private final Map<String, ClientEntry> clients = new ConcurrentHashMap<>();
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
    public HttpClient getClient(CXFClientInfo clientInfo, HttpVersion version, TlsConfiguration tlsConfiguration) {
        final String key = clientInfo.getConfigKey();
        Objects.requireNonNull(key, "CXFClientInfo.configKey cannot be null");
        return clients.computeIfAbsent(key, v -> {
            final HttpClientOptions opts = new HttpClientOptions()
                    .setProtocolVersion(version);
            clientInfo.getVertxConfig().configure(opts, clientInfo.getConnection());

            HttpClientPoolRecorder.configure(clientInfo, opts);

            if (tlsConfiguration != null) {
                TlsConfigUtils.configure(opts, tlsConfiguration);
                return new ClientEntry(vertx.createHttpClient(opts), tlsConfiguration.getName());
            } else {
                return new ClientEntry(vertx.createHttpClient(opts), null);
            }
        }).httpClient;
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
            final Iterator<Entry<String, ClientEntry>> it = clients.entrySet().iterator();
            while (it.hasNext()) {
                final Entry<String, ClientEntry> en = it.next();
                if (tlsConfigName.equals(en.getValue().tlsConfigurationName)) {
                    final HttpClient cl = en.getValue().httpClient();
                    it.remove();
                    cl.close(r -> {
                    });
                }
            }
        }
    }

    public Vertx getVertx() {
        return vertx;
    }

    static record ClientEntry(HttpClient httpClient, String tlsConfigurationName) {
    }

    @Recorder
    public static class HttpClientPoolRecorder {
        private static final List<BiConsumer<CXFClientInfo, HttpClientOptions>> customizers = new ArrayList<>();

        public void addHttpClientCustomizer(RuntimeValue<BiConsumer<CXFClientInfo, HttpClientOptions>> customizer) {
            customizers.add(customizer.getValue());
        }

        public static void configure(CXFClientInfo clientInfo, HttpClientOptions opts) {
            for (BiConsumer<CXFClientInfo, HttpClientOptions> consumer : customizers) {
                consumer.accept(clientInfo, opts);
            }
        }
    }

}
