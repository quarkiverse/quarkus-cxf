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
import io.quarkiverse.cxf.ProxyConfigurationUtils;
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
     * @param clientInfo the client configuration
     * @return a possibly pooled client
     */
    public HttpClient getClient(CXFClientInfo clientInfo, HttpVersion version, TlsConfiguration tlsConfiguration,
            ProxyConfiguration proxyConfiguration) {
        final String key = clientInfo.getConfigKey();
        Objects.requireNonNull(key, "CXFClientInfo.configKey cannot be null");
        return clients.computeIfAbsent(key, v -> {
            final List<HttpVersion> versions = clientInfo.getHttpVersions();
            /* The versions transported over TCP; HTTP/3 is transported over QUIC */
            final List<HttpVersion> tcpVersions = tcpVersions(versions);
            final HttpClientOptions opts = new HttpClientOptions();
            if (versions.isEmpty()) {
                opts.setProtocolVersion(version);
            } else if (!tcpVersions.isEmpty()) {
                opts.setProtocolVersion(tcpVersions.get(0));
            }
            final PoolOptions poolOptions = new PoolOptions();
            clientInfo.getVertxConfig().configure(opts, clientInfo.getConnection(), poolOptions);
            /* The proxy configuration named none has to be ignored */
            if (ProxyConfigurationUtils.isProxyDefined(proxyConfiguration)) {
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
            }
            if (!tcpVersions.isEmpty()) {
                /*
                 * HttpClientConfig(HttpClientOptions) takes an explicit ALPN list verbatim, without expanding
                 * protocolVersion into implicit fallback versions. Keep the options adapter to preserve customizers,
                 * including forceSni, which HttpClientBuilder.with(HttpClientConfig) otherwise resets.
                 * Vert.x enables ALPN only for TLS connections whose list contains HTTP/2.
                 */
                opts.setProtocolVersion(tcpVersions.get(0))
                        .setAlpnVersions(tcpVersions)
                        .setUseAlpn(true);
                opts.getHttp2Config().setClearTextUpgrade(tcpVersions.size() > 1);
            }
            // Vert.x owns the bodyless OPTIONS exchange, upgrade, and fallback, including legacy HTTP/2 clients.
            opts.getHttp2Config().setClearTextUpgradeWithPreflightRequest(true);
            final HttpClientBuilder builder = vertx.httpClientBuilder().with(opts);
            if (versions.contains(HttpVersion.HTTP_3)) {
                /*
                 * HTTP/3 cannot be configured through HttpClientOptions. We still pass the HttpClientOptions to the
                 * builder first, because that is the only way to make it use the TLS settings and the SSL engine
                 * selected through the options and its customizers. The HttpClientConfig passed second replaces the
                 * one which the builder derived from the HttpClientOptions. Doing so resets forceSni in the builder.
                 * forceSni is only relevant for TCP connections to hosts whose names contain no dot.
                 */
                builder.with(toHttp3Config(opts, versions, clientInfo.getVertxConfig().http3KeepAliveTimeout(),
                        clientInfo.getConnectionTimeout()));
            }
            return new ClientEntry(builder.with(poolOptions).build(),
                    tlsConfiguration != null ? tlsConfiguration.getName() : null);
        }).httpClient;
    }

    static List<HttpVersion> tcpVersions(List<HttpVersion> versions) {
        return versions.contains(HttpVersion.HTTP_3)
                ? versions.stream().filter(version -> version != HttpVersion.HTTP_3).toList()
                : versions;
    }

    /**
     * Adapts the given {@link HttpClientOptions} to a {@link HttpClientConfig} supporting the given {@code versions}
     * containing {@link HttpVersion#HTTP_3}.
     * Protocol discovery, QUIC probing and the fallback to TCP based versions are left to Vert.x {@link HttpClient}.
     *
     * @param opts the fully customized {@link HttpClientOptions} whose settings should be taken over;
     *        their ALPN versions must not contain {@link HttpVersion#HTTP_3}
     * @param versions the ordered list of permitted HTTP versions containing {@link HttpVersion#HTTP_3}
     * @param http3KeepAliveTimeout the number of seconds after which idle HTTP/3 connections should be closed; {@code 0}
     *        means no timeout
     * @param connectionTimeout the number of milliseconds to wait for a QUIC connection to get established; {@code 0}
     *        means no timeout
     * @return a new {@link HttpClientConfig}
     */
    static HttpClientConfig toHttp3Config(HttpClientOptions opts, List<HttpVersion> versions, int http3KeepAliveTimeout,
            long connectionTimeout) {
        /*
         * The constructor takes over everything that can be expressed through HttpClientOptions, most notably the
         * TCP, HTTP/1.x, HTTP/2 and metrics settings. The rest of this method is about what is specific to HTTP/3.
         */
        final HttpClientConfig config = new HttpClientConfig(opts);
        config.setVersions(versions);
        /*
         * A sole HTTP/3 means HTTP/3 right away for every request and no fallback. With more versions, Vert.x contacts
         * the service over TCP first and it may use HTTP/3 only for alternative services advertised by the service
         * via Alt-Svc. That needs to be enabled explicitly; otherwise HTTP/3 would never be used.
         */
        config.setFollowAlternativeServices(versions.size() > 1);
        /* The HTTP/1.x and HTTP/2 keep alive timeouts were taken over from opts above */
        config.getHttp3Config().setKeepAliveTimeout(Duration.ofSeconds(http3KeepAliveTimeout));
        /*
         * This limit is fixed for the lifetime of the pooled client. The connect timeout passed with each request via
         * RequestOptions can only shorten it, because Vert.x does not pass it to QuicConnectOptions.
         * This is documented in CxfClientConfig.VertxConfig.httpVersions()
         */
        config.getQuicConfig().setConnectTimeout(Duration.ofMillis(connectionTimeout));
        return config;
    }

    /**
     * Update clients using the reloaded TLS configuration. Failed updates evict the client for recreation.
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
        /* Written once at static init by a recorder, hence not final */
        private static volatile QuicSupport quicSupport = QuicSupport.QUARKUS_HTTP3_MISSING;

        public void addHttpClientCustomizer(RuntimeValue<BiConsumer<CXFClientInfo, HttpClientOptions>> customizer) {
            customizers.add(customizer.getValue());
        }

        /**
         * @param quicSupport the availability of QUIC in the current application as figured out at build time
         * @since 4.0.0
         */
        public void setQuicSupport(QuicSupport quicSupport) {
            HttpClientPoolRecorder.quicSupport = quicSupport;
        }

        public static void configure(CXFClientInfo clientInfo, HttpClientOptions opts) {
            for (BiConsumer<CXFClientInfo, HttpClientOptions> consumer : customizers) {
                consumer.accept(clientInfo, opts);
            }
        }

        /**
         * @return the availability of QUIC in the current application
         * @since 4.0.0
         */
        public static QuicSupport getQuicSupport() {
            return quicSupport;
        }
    }

}
