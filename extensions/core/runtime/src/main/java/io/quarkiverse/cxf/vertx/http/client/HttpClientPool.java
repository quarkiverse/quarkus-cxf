package io.quarkiverse.cxf.vertx.http.client;

import java.time.Duration;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import io.quarkiverse.cxf.CXFClientInfo;
import io.quarkus.proxy.ProxyConfiguration;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.tls.CertificateUpdatedEvent;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.runtime.config.TlsConfigUtils;
import io.vertx.core.Vertx;
import io.vertx.core.http.*;
import io.vertx.core.net.ProxyOptions;
import io.vertx.core.net.ProxyType;

/**
 * A pool of HTTP clients so that we do not have to reconnect on every request.
 */
@ApplicationScoped
public class HttpClientPool {
    private static final Logger log = Logger.getLogger(HttpClientPool.class);
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
    public HttpClient getClient(CXFClientInfo clientInfo, HttpVersion version, TlsConfiguration tlsConfiguration,
            ProxyConfiguration proxyConfiguration) {
        final String key = clientInfo.getConfigKey();
        Objects.requireNonNull(key, "CXFClientInfo.configKey cannot be null");
        return clients.computeIfAbsent(key, v -> {
            final HttpClientOptions opts = new HttpClientOptions()
                    .setProtocolVersion(version);
            final PoolOptions poolOptions = new PoolOptions();
            clientInfo.getVertxConfig().configure(opts, clientInfo.getConnection(), poolOptions);
            if (proxyConfiguration != null) {
                proxyConfiguration.nonProxyHosts().ifPresent(nph -> nph.forEach(opts::addNonProxyHost));
                final ProxyOptions proxyOpts = new ProxyOptions()
                        .setHost(proxyConfiguration.host())
                        .setPort(proxyConfiguration.port())
                        .setType(toProxyType(proxyConfiguration.type()));
                proxyConfiguration.username().ifPresent(proxyOpts::setUsername);
                proxyConfiguration.password().ifPresent(proxyOpts::setPassword);
                opts.setProxyOptions(proxyOpts);
            }

            HttpClientPoolRecorder.configure(clientInfo, opts);

            if (tlsConfiguration != null) {
                TlsConfigUtils.configure(opts, tlsConfiguration);
                return new ClientEntry(vertx.createHttpClient(opts, poolOptions), tlsConfiguration.getName());
            } else {
                return new ClientEntry(vertx.createHttpClient(opts, poolOptions), null);
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
        final TlsConfiguration updatedTlsConfiguration = event.tlsConfiguration();
        final Map<String, ClientEntry> clientsToUpdate = new LinkedHashMap<>();
        for (Entry<String, ClientEntry> en : clients.entrySet()) {
            if (tlsConfigName.equals(en.getValue().tlsConfigurationName)) {
                clientsToUpdate.put(en.getKey(), en.getValue());
            }
        }

        if (!clientsToUpdate.isEmpty()) {
            for (Entry<String, ClientEntry> en : clientsToUpdate.entrySet()) {
                ((HttpClientAgent) en.getValue().httpClient)
                        .updateSSLOptions(updatedTlsConfiguration.getClientSSLOptions())
                        .onComplete(event1 -> {
                            if (event1.succeeded()) {
                                if (event1.result()) {
                                    log.infof(
                                            "Certificate reloaded for the SOAP client '%s' using the TLS configuration (bucket) name '%s'",
                                            en.getKey(),
                                            tlsConfigName);
                                } else {
                                    log.warnf(
                                            "Certificate reload skipped for the SOAP client '%s' using the TLS configuration (bucket) name '%s'",
                                            en.getKey(),
                                            tlsConfigName);
                                }
                            } else {
                                final Duration graceTimeout = Duration.ofSeconds(30);
                                log.errorf(event1.cause(),
                                        "Certificate reload failed for the SOAP client '%s' using the TLS configuration (bucket) name '%s'. The client will be shutdown",
                                        en.getKey(),
                                        tlsConfigName);
                                clients.remove(en.getKey());
                                en.getValue().httpClient.shutdown(graceTimeout)
                                        .onComplete(h -> {
                                        });
                            }
                        });
            }

        }

    }

    public Vertx getVertx() {
        return vertx;
    }

    static ProxyType toProxyType(io.quarkus.proxy.ProxyType type) {
        switch (type) {
            case HTTP:
                return ProxyType.HTTP;
            case SOCKS4:
                return ProxyType.SOCKS4;
            case SOCKS5:
                return ProxyType.SOCKS5;
            default:
                throw new IllegalArgumentException("Unexpected " + io.quarkus.proxy.ProxyType.class.getName() + " " + type);
        }
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
