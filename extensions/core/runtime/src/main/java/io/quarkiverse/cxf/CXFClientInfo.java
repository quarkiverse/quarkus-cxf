package io.quarkiverse.cxf;

import java.net.Proxy.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.cxf.annotations.SchemaValidation.SchemaValidationType;
import org.apache.cxf.transports.http.configuration.ConnectionType;
import org.apache.cxf.transports.http.configuration.ProxyServerType;
import org.jboss.logging.Logger;

import io.quarkiverse.cxf.CxfClientConfig.Auth;
import io.quarkiverse.cxf.CxfClientConfig.VertxConfig;
import io.quarkiverse.cxf.CxfConfig.CxfGlobalClientConfig;
import io.quarkiverse.cxf.CxfConfig.RetransmitCacheConfig;
import io.quarkiverse.cxf.vertx.http.client.HttpClientPool.HttpClientPoolRecorder;
import io.quarkiverse.cxf.vertx.http.client.QuicSupport;
import io.quarkus.arc.Arc;
import io.quarkus.arc.Unremovable;
import io.quarkus.proxy.ProxyConfiguration;
import io.quarkus.proxy.ProxyConfigurationRegistry;
import io.quarkus.proxy.ProxyType;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpVersion;

/**
 * CXF client metadata - the complete set as known at runtime.
 */
@Unremovable
public class CXFClientInfo {
    private static final Logger log = Logger.getLogger(CXFClientInfo.class);
    private static final String JAVA_NET_SSL_TLS_CONFIGURATION_NAME = "javax.net.ssl";

    private final String sei;
    private final String endpointAddress;
    private final String wsdlUrl;
    private final String soapBinding;
    private final String wsNamespace;
    private final String wsName;
    private final String epNamespace;
    private final String epName;
    private final Auth auth;
    private final VertxConfig vertxConfig;
    private final boolean proxyClassRuntimeInitialized;
    private final List<String> inInterceptors = new ArrayList<>();
    private final List<String> outInterceptors = new ArrayList<>();
    private final List<String> outFaultInterceptors = new ArrayList<>();
    private final List<String> inFaultInterceptors = new ArrayList<>();
    private final List<String> features = new ArrayList<>();
    private final List<String> handlers = new ArrayList<>();

    /* org.apache.cxf.transports.http.configuration.HTTPClientPolicy attributes */
    /**
     * Specifies the amount of time, in milliseconds, that the consumer will attempt to establish a connection before it
     * times
     * out. 0 is infinite.
     */
    private final long connectionTimeout;
    /**
     * Specifies the amount of time, in milliseconds, that the consumer will wait for a response before it times out. 0
     * is
     * infinite.
     */
    private final long receiveTimeout;
    /**
     * Specifies the amount of time, in milliseconds, used when requesting a connection from the connection manager(if
     * appliable). 0 is infinite.
     */
    private final long connectionRequestTimeout;
    /**
     * Specifies if the consumer will automatically follow a server issued redirection.
     * (name is not part of standard)
     */
    private final boolean autoRedirect;

    /**
     * If `true` relative URIs, such as `/folder/service` received via `Location` response header will be honored when
     * redirecting; otherwise only absolute URIs will be honored and an exception will be thrown for relative redirects.
     *
     * This is equivalent to setting `http.redirect.relative.uri` property to `true` on the CXF client request context.
     */
    private final boolean redirectRelativeUri;
    /**
     * Specifies the maximum amount of retransmits that are allowed for redirects. Retransmits for
     * authorization is included in the retransmit count. Each redirect may cause another
     * retransmit for a UNAUTHORIZED response code, ie. 401.
     * Any negative number indicates unlimited retransmits,
     * although, loop protection is provided.
     * The default is unlimited.
     * (name is not part of standard)
     */
    private final int maxRetransmits;

    private final RetransmitCacheConfig retransmitCache;

    /**
     * Specifies the maximum amount of retransmits to the same uri that are allowed for redirects.
     * Retransmits for authorization is included in the retransmit count. Each redirect may cause another
     * retransmit for a UNAUTHORIZED response code, ie. 401. Any negative number indicates 0 retransmits
     * to the same uri allowed.
     * The default is 0.
     *
     * This is equivalent to setting `http.redirect.max.same.uri.count` property on the CXF client request context.
     */
    private final int maxSameUri;
    /**
     * If true, the client is free to use chunking streams if it wants, but it is not
     * required to use chunking streams. If false, the client
     * must use regular, non-chunked requests in all cases.
     */
    private final boolean allowChunking;
    /**
     * If AllowChunking is true, this sets the threshold at which messages start
     * getting chunked. Messages under this limit do not get chunked.
     */
    private final int chunkingThreshold;
    /**
     * Specifies the chunk length for a HttpURLConnection. This value is used in
     * java.net.HttpURLConnection.setChunkedStreamingMode(int chunklen). chunklen indicates the number of bytes to write
     * in each
     * chunk. If chunklen is less than or equal to zero, a default value will be used.
     */
    private final int chunkLength;
    /**
     * Specifies the MIME types the client is prepared to handle (e.g., HTML, JPEG, GIF, etc.)
     */
    private final String accept;
    /**
     * Specifies the language the client desires (e.g., English, French, etc.)
     */
    private final String acceptLanguage;
    /**
     * Specifies the encoding the client is prepared to handle (e.g., gzip)
     */
    private final String acceptEncoding;
    /**
     * Specifies the content type of the stream being sent in a post request.
     * (this should be text/xml for web services, or can be set to
     * application/x-www-form-urlencoded if the client is sending form data).
     */
    private final String contentType;
    /**
     * Specifies the Internet host and port number of the resource on which the request is being invoked.
     * This is sent by default based upon the URL. Certain DNS scenarios or
     * application designs may request you to set this, but typically it is
     * not required.
     */
    private final String host;
    /**
     * The connection disposition. If close the connection to the server is closed
     * after each request/response dialog. If Keep-Alive the client requests the server
     * to keep the connection open, and if the server honors the keep alive request,
     * the connection is reused. Many servers and proxies do not honor keep-alive requests.
     *
     */
    private final ConnectionType connection;
    /**
     * Most commonly used to specify no-cache, however the standard supports a
     * dozen or so caching related directives for requests
     */
    private final String cacheControl;
    /**
     * HTTP Version used for the connection. The "auto" default will use whatever the default is
     * for the HTTPConduit implementation.
     */
    private final String version;
    private final List<HttpVersion> httpVersions;
    /**
     * aka User-Agent
     * Specifies the type of browser is sending the request. This is usually only
     * needed when sites have HTML customized to Netscape vs IE, etc, but can
     * also be used to optimize for different SOAP stacks.
     */
    private final String browserType;
    /**
     * Specifies the URL of a decoupled endpoint for the receipt of responses over a separate provider->consumer
     * connection.
     */
    private final String decoupledEndpoint;
    private final String decoupledEndpointBase;

    private final ProxyConfiguration proxyConfiguration;
    /**
     * Decides whether a host is accessed through the proxy defined by {@link #proxyConfiguration}.
     * Kept here, so that the patterns in {@link ProxyConfiguration#nonProxyHosts()} are compiled only once per client
     * rather than on each request.
     */
    private final Predicate<String> proxiedHosts;

    private final String tlsConfigurationName;

    private final TlsConfiguration tlsConfiguration;

    private final String hostnameVerifier;

    private final HTTPConduitImpl httpConduitImpl;

    private final String configKey;

    private final SchemaValidationType schemaValidationEnabledFor;

    private final boolean secureWsdlAccess;
    private final long workerDispatchTimeout;

    /*
     * FIXME: (review) AGENTS.md, section JavaDoc: "Public and protected types and type members must have JavaDoc".
     * The contract of this public constructor changed in this commit: configKey must not be null anymore
     * and an IllegalStateException is thrown if client-endpoint-url is not set.
     * Could you please document that here (@param configKey ..., @throws IllegalStateException ..., @since)?
     */
    public CXFClientInfo(CXFClientData other, CxfConfig cxfConfig, CxfClientConfig config, String configKey, Vertx vertx) {
        Objects.requireNonNull(config);
        Objects.requireNonNull(configKey, "configKey");
        this.sei = other.getSei();
        this.soapBinding = config.soapBinding().orElse(other.getSoapBinding());
        this.wsName = other.getWsName();
        this.wsNamespace = other.getWsNamespace();
        this.proxyClassRuntimeInitialized = other.isProxyClassRuntimeInitialized();
        this.epNamespace = config.endpointNamespace().orElse(null);
        this.epName = config.endpointName().orElse(null);
        this.auth = new AuthWrapper(config.auth(), config);
        this.vertxConfig = config.vertx();
        this.secureWsdlAccess = config.secureWsdlAccess();
        this.endpointAddress = requireEndpointAddress(config, configKey, this.sei);
        this.wsdlUrl = config.wsdlPath().orElse(null);
        addFeatures(config);
        addHandlers(config);
        addInterceptors(config);
        this.connectionTimeout = config.connectionTimeout();
        this.receiveTimeout = config.receiveTimeout();
        this.connectionRequestTimeout = config.connectionRequestTimeout();
        this.autoRedirect = config.autoRedirect();
        this.redirectRelativeUri = config.redirectRelativeUri();
        this.maxRetransmits = config.maxRetransmits();
        this.maxSameUri = config.maxSameUri();
        this.retransmitCache = cxfConfig.retransmitCache();
        this.allowChunking = config.allowChunking();
        this.chunkingThreshold = config.chunkingThreshold();
        this.chunkLength = config.chunkLength();
        this.accept = config.accept().orElse(null);
        this.acceptLanguage = config.acceptLanguage().orElse(null);
        this.acceptEncoding = config.acceptEncoding().orElse(null);
        this.contentType = config.contentType().orElse(null);
        this.host = config.host().orElse(null);
        this.connection = config.connection();
        this.cacheControl = config.cacheControl().orElse(null);
        this.version = config.version();
        this.httpVersions = parseHttpVersions(config, configKey);
        this.browserType = config.browserType().orElse(null);
        this.decoupledEndpoint = config.decoupledEndpoint().orElse(null);
        this.decoupledEndpointBase = cxfConfig.decoupledEndpointBase().orElse(null);
        final ProxyConfigurationRegistry registry = Arc.container().select(ProxyConfigurationRegistry.class).get();
        this.proxyConfiguration = registry.get(config.proxyConfigurationName()).orElse(null);
        this.proxiedHosts = ProxyConfigurationUtils.proxiedHosts(proxyConfiguration);
        this.tlsConfigurationName = config.tlsConfigurationName().orElse(null);
        this.tlsConfiguration = tlsConfiguration(vertx, cxfConfig.client(), config, configKey);
        this.hostnameVerifier = config.hostnameVerifier().orElse(null);
        this.schemaValidationEnabledFor = config.schemaValidationEnabledFor().orElse(null);
        this.workerDispatchTimeout = cxfConfig.client().workerDispatchTimeout();

        /*
         * If the optional is empty, this.httpConduitImpl will be null.
         * In that case, the HTTPConduitFactory set on the Bus based on quarkus.cxf.http-conduit-impl
         * should kick in.
         */
        this.httpConduitImpl = config.httpConduitFactory().orElse(null);
        this.configKey = configKey;

        if (httpVersions.contains(HttpVersion.HTTP_3)) {
            final Supplier<String> property = this::httpVersionsProperty;
            validateHttp3Destination(property, httpVersions, proxyConfiguration, proxiedHosts,
                    toRemoteUri(endpointAddress));
            validateHttp3Destination(property, httpVersions, proxyConfiguration, proxiedHosts, toRemoteUri(wsdlUrl));
            validateQuicSupport(property, httpVersions, HttpClientPoolRecorder.getQuicSupport());
        }
    }

    /**
     * @param config the configuration of the given client
     * @param configKey the name of the given client, such as {@code client-name} in
     *        {@code quarkus.cxf.client."client-name".client-endpoint-url}
     * @param sei the fully qualified class name of the Service Endpoint Interface of the given client
     * @return the value of {@code quarkus.cxf.client."client-name".client-endpoint-url}
     * @throws IllegalStateException if {@code quarkus.cxf.client."client-name".client-endpoint-url} is not set
     */
    static String requireEndpointAddress(CxfClientConfig config, String configKey, String sei) {
        return config.clientEndpointUrl().orElseThrow(() -> {
            final String key = "quarkus.cxf.client." + QuarkusCxfUtils.quoteCongurationKeyIfNeeded(configKey);
            return new IllegalStateException("Cannot create the client " + configKey + " for the service interface " + sei
                    + ", because " + key + ".client-endpoint-url is not set; set " + key
                    + ".client-endpoint-url to the URL of the remote service endpoint");
        });
    }

    static List<HttpVersion> parseHttpVersions(CxfClientConfig config, String configKey) {
        final Optional<List<String>> configured = config.vertx().httpVersions();
        if (configured.isEmpty()) {
            return List.of();
        }
        final String key = "quarkus.cxf.client." + QuarkusCxfUtils.quoteCongurationKeyIfNeeded(configKey);
        if (!"auto".equals(config.version())) {
            throw new IllegalArgumentException(key + ".vertx.http-versions cannot be combined with " + key
                    + ".version = " + config.version() + "; leave version at auto");
        }
        final List<HttpVersion> versions = new ArrayList<>();
        final List<String> values = configured.get();
        if (values.size() == 1) {
            return List.of(parseHttpVersion(key, values.get(0)));
        }
        for (String value : values) {
            final HttpVersion version = parseHttpVersion(key, value);
            if (versions.contains(version)) {
                throw new IllegalArgumentException("Duplicate HTTP version '" + value + "' in " + key + ".vertx.http-versions");
            }
            versions.add(version);
        }
        if (versions.isEmpty()) {
            throw new IllegalArgumentException(key + ".vertx.http-versions must contain at least one HTTP version");
        }
        return List.copyOf(versions);
    }

    private static HttpVersion parseHttpVersion(String key, String value) {
        return switch (value) {
            case "1.0" -> HttpVersion.HTTP_1_0;
            case "1.1" -> HttpVersion.HTTP_1_1;
            case "2" -> HttpVersion.HTTP_2;
            case "3" -> HttpVersion.HTTP_3;
            default -> throw new IllegalArgumentException("Unsupported HTTP version '" + value + "' in " + key
                    + ".vertx.http-versions; supported values are 1.0, 1.1, 2 and 3");
        };
    }

    private static String toConfigValue(List<HttpVersion> versions) {
        return versions.stream()
                .map(version -> switch (version) {
                    case HTTP_1_0 -> "1.0";
                    case HTTP_1_1 -> "1.1";
                    case HTTP_2 -> "2";
                    case HTTP_3 -> "3";
                })
                .collect(Collectors.joining(","));
    }

    public String httpVersionsProperty() {
        return "quarkus.cxf.client." + QuarkusCxfUtils.quoteCongurationKeyIfNeeded(configKey) + ".vertx.http-versions";
    }

    /** Validate the effective destination, without including credentials or query parameters in diagnostics. */
    public void validateHttpVersions(URI destination) {
        /*
         * The HTTP/3 check has to come first: lists containing 3 are rejected for any plain http: destination, so they
         * never reach the check below, whose message is right only for 2,1.0
         */
        validateHttp3Destination(this::httpVersionsProperty, httpVersions, proxyConfiguration, proxiedHosts, destination);
        if (httpVersions.size() > 1 && httpVersions.get(0) == HttpVersion.HTTP_2
                && !httpVersions.contains(HttpVersion.HTTP_1_1) && "http".equalsIgnoreCase(destination.getScheme())) {
            throw new IllegalArgumentException(httpVersionsProperty()
                    + " = 2,1.0 cannot be used with plaintext HTTP; use 2 for prior knowledge or include 1.1 for h2c upgrade");
        }
    }

    /**
     * Rejects the combinations of HTTP/3 and the given {@code destination} for which Vert.x {@code HttpClient} would
     * silently do something else than what was configured:
     * <ul>
     * <li>Plain {@code http:} destinations: with {@code 3} alone, Vert.x would connect over QUIC (and thus TLS) anyway.
     * With {@code 3} combined with other versions, HTTP/3 would never be used.
     * <li>Destinations accessed through a proxy: with {@code 3} alone, Vert.x would bypass the proxy and connect to
     * the destination directly. With {@code 3} combined with other versions, HTTP/3 would never be used, because
     * Vert.x does not follow alternative service advertisements received through a proxy.
     * </ul>
     *
     * @param httpVersionsProperty the name of the {@code http-versions} configuration property to use in exception messages
     * @param httpVersions the HTTP versions configured for the given client
     * @param proxyConfiguration the {@link ProxyConfiguration} of the given client or {@code null} if there is none
     * @param proxiedHosts the result of {@link ProxyConfigurationUtils#proxiedHosts(ProxyConfiguration)} for the given
     *        {@code proxyConfiguration}
     * @param destination the URI to validate; nothing is validated if {@code null}
     * @throws IllegalArgumentException if HTTP/3 cannot be used with the given {@code destination}
     */
    static void validateHttp3Destination(Supplier<String> httpVersionsProperty, List<HttpVersion> httpVersions,
            ProxyConfiguration proxyConfiguration, Predicate<String> proxiedHosts, URI destination) {
        if (destination == null || !httpVersions.contains(HttpVersion.HTTP_3)) {
            return;
        }
        if (!"https".equalsIgnoreCase(destination.getScheme())) {
            throw new IllegalArgumentException(httpVersionsProperty.get() + " = " + toConfigValue(httpVersions)
                    + " cannot be used with " + origin(destination) + ", because HTTP/3 requires an https: URL;"
                    + " use an https: URL or remove 3 from " + httpVersionsProperty.get());
        }
        if (proxiedHosts.test(destination.getHost())) {
            throw new IllegalArgumentException(httpVersionsProperty.get() + " = " + toConfigValue(httpVersions)
                    + " cannot be used with " + origin(destination) + ", because that destination would be accessed"
                    + " through the proxy " + proxyConfiguration.host() + ":" + proxyConfiguration.port()
                    + " and HTTP/3 through a proxy is not supported;"
                    + " remove 3 from " + httpVersionsProperty.get()
                    + ", or add " + destination.getHost() + " to non-proxy-hosts of the given proxy configuration"
                    + ", or set " + httpVersionsProperty.get().replace(".vertx.http-versions", ".proxy-configuration-name")
                    + " to none");
        }
    }

    /**
     * @param httpVersionsProperty the name of the {@code http-versions} configuration property to use in exception messages
     * @param httpVersions the HTTP versions configured for the given client
     * @param quicSupport the availability of QUIC in the current application
     * @throws IllegalStateException if the given {@code httpVersions} contain HTTP/3 and QUIC is not available
     */
    static void validateQuicSupport(Supplier<String> httpVersionsProperty, List<HttpVersion> httpVersions,
            QuicSupport quicSupport) {
        if (!httpVersions.contains(HttpVersion.HTTP_3)) {
            return;
        }
        switch (quicSupport) {
            case AVAILABLE:
                return;
            case QUARKUS_HTTP3_MISSING:
                throw new IllegalStateException(httpVersionsProperty.get() + " = " + toConfigValue(httpVersions)
                        + " requires QUIC, which is not available in this application;"
                        + " add the io.quarkus:quarkus-http3 extension to the application"
                        + " and set quarkus.http3.enabled = false unless the HTTP server of the application should support HTTP/3 too");
            case NATIVE_LIBRARY_MISSING:
                throw new IllegalStateException(httpVersionsProperty.get() + " = " + toConfigValue(httpVersions)
                        + " requires QUIC, but no native QUIC library was found in the application;"
                        + " add the io.netty:netty-codec-native-quic dependency with the classifier matching your platform"
                        + " (such as linux-x86_64 or osx-aarch_64) to the application");
            default:
                throw new IllegalStateException("Unexpected " + QuicSupport.class.getName() + ": " + quicSupport);
        }
    }

    /**
     * @param uri a URI or a classpath resource path or a file system path
     * @return a {@link URI} if the given {@code uri} is an {@code http:} or {@code https:} URI; otherwise {@code null}
     */
    static URI toRemoteUri(String uri) {
        if (uri == null) {
            return null;
        }
        try {
            final URI result = new URI(uri);
            final String scheme = result.getScheme();
            return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme) ? result : null;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /** @return scheme, host and port of the given {@code uri}, thus leaving out any credentials, path and query */
    private static String origin(URI uri) {
        final int port = uri.getPort();
        return uri.getScheme() + "://" + uri.getHost() + (port >= 0 ? ":" + port : "");
    }

    static TlsConfiguration tlsConfiguration(Vertx vertx, CxfGlobalClientConfig globalConfig, CxfClientConfig config,
            String configKey) {
        final TlsConfigurationRegistry tlsRegistry = Arc.container().select(TlsConfigurationRegistry.class).get();
        final Optional<String> maybeTlsConfigName = config.tlsConfigurationName();
        if (maybeTlsConfigName.isEmpty()) {
            /* use global client tls configuration */
            Optional<TlsConfiguration> maybeTlsConfig = tlsRegistry.get(globalConfig.tlsConfigurationName());
            if (maybeTlsConfig.isPresent()) {
                return maybeTlsConfig.get();
            } else {
                throw new IllegalStateException(
                        "No such TLS configuration quarkus.tls." + globalConfig.tlsConfigurationName());
            }
        } else {
            /* tls-configuration-name is set */
            Optional<TlsConfiguration> maybeTlsConfig = tlsRegistry.get(maybeTlsConfigName.get());
            if (maybeTlsConfig.isPresent()) {
                return maybeTlsConfig.get();
            } else {
                throw new IllegalStateException("No such TLS configuration quarkus.tls." + maybeTlsConfigName.get());
            }
        }
    }

    public String getHostnameVerifier() {
        return hostnameVerifier;
    }

    public String getSei() {
        return sei;
    }

    public String getEndpointAddress() {
        return endpointAddress;
    }

    public String getWsdlUrl() {
        return wsdlUrl;
    }

    public String getSoapBinding() {
        return soapBinding;
    }

    public String getWsNamespace() {
        return wsNamespace;
    }

    public String getWsName() {
        return wsName;
    }

    public String getEpNamespace() {
        return epNamespace;
    }

    public String getEpName() {
        return epName;
    }

    public Auth getAuth() {
        return auth;
    }

    public VertxConfig getVertxConfig() {
        return vertxConfig;
    }

    /**
     * @return the explicitly configured ordered versions, or an empty list to use the legacy version selection
     */
    public List<HttpVersion> getHttpVersions() {
        return httpVersions;
    }

    public boolean isProxyClassRuntimeInitialized() {
        return proxyClassRuntimeInitialized;
    }

    public List<String> getFeatures() {
        return features;
    }

    public List<String> getHandlers() {
        return handlers;
    }

    public List<String> getInInterceptors() {
        return inInterceptors;
    }

    public List<String> getOutInterceptors() {
        return outInterceptors;
    }

    public List<String> getOutFaultInterceptors() {
        return outFaultInterceptors;
    }

    public List<String> getInFaultInterceptors() {
        return inFaultInterceptors;
    }

    private CXFClientInfo addInterceptors(CxfClientConfig cxfEndPointConfig) {
        if (cxfEndPointConfig.inInterceptors().isPresent()) {
            this.inInterceptors.addAll(cxfEndPointConfig.inInterceptors().get());
        }
        if (cxfEndPointConfig.outInterceptors().isPresent()) {
            this.outInterceptors.addAll(cxfEndPointConfig.outInterceptors().get());
        }
        if (cxfEndPointConfig.outFaultInterceptors().isPresent()) {
            this.outFaultInterceptors.addAll(cxfEndPointConfig.outFaultInterceptors().get());
        }
        if (cxfEndPointConfig.inFaultInterceptors().isPresent()) {
            this.inFaultInterceptors.addAll(cxfEndPointConfig.inFaultInterceptors().get());
        }
        return this;
    }

    private CXFClientInfo addFeatures(CxfClientConfig cxfEndPointConfig) {
        if (cxfEndPointConfig.features().isPresent()) {
            this.features.addAll(cxfEndPointConfig.features().get());
        }
        return this;
    }

    private CXFClientInfo addHandlers(CxfClientConfig cxfEndPointConfig) {
        if (cxfEndPointConfig.handlers().isPresent()) {
            this.handlers.addAll(cxfEndPointConfig.handlers().get());
        }
        return this;
    }

    public Long getConnectionTimeout() {
        return connectionTimeout;
    }

    public Long getReceiveTimeout() {
        return receiveTimeout;
    }

    public Long getConnectionRequestTimeout() {
        return connectionRequestTimeout;
    }

    public boolean isAutoRedirect() {
        return autoRedirect;
    }

    public boolean isRedirectRelativeUri() {
        return redirectRelativeUri;
    }

    public RetransmitCacheConfig getRetransmitCache() {
        return retransmitCache;
    }

    public int getMaxRetransmits() {
        return maxRetransmits;
    }

    public int getMaxSameUri() {
        return maxSameUri;
    }

    public boolean isAllowChunking() {
        return allowChunking;
    }

    public int getChunkingThreshold() {
        return chunkingThreshold;
    }

    public int getChunkLength() {
        return chunkLength;
    }

    public String getAccept() {
        return accept;
    }

    public String getAcceptLanguage() {
        return acceptLanguage;
    }

    public String getAcceptEncoding() {
        return acceptEncoding;
    }

    public String getContentType() {
        return contentType;
    }

    public String getHost() {
        return host;
    }

    public ConnectionType getConnection() {
        return connection;
    }

    public String getCacheControl() {
        return cacheControl;
    }

    public String getVersion() {
        return version;
    }

    public String getBrowserType() {
        return browserType;
    }

    public String getDecoupledEndpoint() {
        return decoupledEndpoint;
    }

    public String getDecoupledEndpointBase() {
        return decoupledEndpointBase;
    }

    public ProxyConfiguration getProxyConfiguration() {
        return proxyConfiguration;
    }

    public HTTPConduitImpl getHttpConduitImpl() {
        return httpConduitImpl;
    }

    public String getTlsConfigurationName() {
        return tlsConfigurationName;
    }

    public TlsConfiguration getTlsConfiguration() {
        return tlsConfiguration;
    }

    public String getConfigKey() {
        return configKey;
    }

    public SchemaValidationType getSchemaValidationEnabledFor() {
        return schemaValidationEnabledFor;
    }

    public boolean isSecureWsdlAccess() {
        return secureWsdlAccess;
    }

    public long getWorkerDispatchTimeout() {
        return workerDispatchTimeout;
    }

    static ProxyType toQuarkusProxyType(ProxyServerType proxyServerType) {
        switch (proxyServerType) {
            case HTTP:
                return ProxyType.HTTP;
            case SOCKS:
                return ProxyType.SOCKS4;
            default:
                throw new IllegalArgumentException("Unexpected " + Type.class.getName() + " " + proxyServerType);
        }
    }

    static class AuthWrapper implements Auth {
        private final Auth auth;
        private final CxfClientConfig config;

        public AuthWrapper(Auth auth, CxfClientConfig config) {
            super();
            this.auth = auth;
            this.config = config;
        }

        @Override
        public Optional<String> username() {
            return auth.username();
        }

        @Override
        public Optional<String> password() {
            return auth.password();
        }

        @Override
        public Optional<String> scheme() {
            return auth.scheme();
        }

        @Override
        public Optional<String> token() {
            return auth.token();
        }
    }

}
