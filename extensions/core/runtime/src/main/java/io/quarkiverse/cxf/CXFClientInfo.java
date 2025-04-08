package io.quarkiverse.cxf;

import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.apache.cxf.annotations.SchemaValidation.SchemaValidationType;
import org.apache.cxf.transports.http.configuration.ConnectionType;
import org.apache.cxf.transports.http.configuration.ProxyServerType;

import io.quarkiverse.cxf.CxfClientConfig.Auth;
import io.quarkiverse.cxf.CxfConfig.CxfGlobalClientConfig;
import io.quarkiverse.cxf.CxfConfig.RetransmitCacheConfig;
import io.quarkus.arc.Arc;
import io.quarkus.arc.Unremovable;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.KeyStoreOptionsBase;
import io.vertx.core.net.PfxOptions;

/**
 * CXF client metadata - the complete set as known at runtime.
 */
@Unremovable
public class CXFClientInfo {
    private static final String DEFAULT_EP_ADDR = "http://localhost:8080";
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
    /**
     * Specifies the address of proxy server if one is used.
     */
    private final String proxyServer;
    /**
     * Specifies the port number used by the proxy server.
     */
    private final Integer proxyServerPort;
    /**
     * Specifies the list of hostnames that will not use the proxy configuration.
     * Examples of value:
     * * "localhost" -> A single hostname
     * * "localhost|www.google.com" -> 2 hostnames that will not use the proxy configuration
     * * "localhost|www.google.*|*.apache.org" -> It's also possible to use a pattern-like value
     */
    private final String nonProxyHosts;
    /**
     * Specifies the type of the proxy server. Can be either HTTP or SOCKS.
     */
    private final ProxyServerType proxyServerType;
    /**
     * Username for the proxy authentication
     */
    private final String proxyUsername;
    /**
     * Password for the proxy authentication
     */
    private final String proxyPassword;

    private final String tlsConfigurationName;

    private final TlsConfiguration tlsConfiguration;

    private final String hostnameVerifier;

    private final HTTPConduitImpl httpConduitImpl;

    private final String configKey;

    private final SchemaValidationType schemaValidationEnabledFor;

    private final boolean secureWsdlAccess;
    private final long workerDispatchTimeout;

    public CXFClientInfo(CXFClientData other, CxfConfig cxfConfig, CxfClientConfig config, String configKey, Vertx vertx) {
        Objects.requireNonNull(config);
        this.sei = other.getSei();
        this.soapBinding = config.soapBinding().orElse(other.getSoapBinding());
        this.wsName = other.getWsName();
        this.wsNamespace = other.getWsNamespace();
        this.proxyClassRuntimeInitialized = other.isProxyClassRuntimeInitialized();
        this.epNamespace = config.endpointNamespace().orElse(null);
        this.epName = config.endpointName().orElse(null);
        this.auth = new AuthWrapper(config.auth(), config);
        this.secureWsdlAccess = config.secureWsdlAccess();
        this.endpointAddress = config.clientEndpointUrl().orElse(DEFAULT_EP_ADDR + "/" + this.sei.toLowerCase());
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
        this.browserType = config.browserType().orElse(null);
        this.decoupledEndpoint = config.decoupledEndpoint().orElse(null);
        this.decoupledEndpointBase = cxfConfig.decoupledEndpointBase().orElse(null);
        this.proxyServer = config.proxyServer().orElse(null);
        this.proxyServerPort = config.proxyServerPort().orElse(null);
        this.nonProxyHosts = config.nonProxyHosts().orElse(null);
        this.proxyServerType = config.proxyServerType();
        this.proxyUsername = config.proxyUsername().orElse(null);
        this.proxyPassword = config.proxyPassword().orElse(null);
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
    }

    static TlsConfiguration tlsConfiguration(Vertx vertx, CxfGlobalClientConfig globalConfig, CxfClientConfig config,
            String configKey) {
        final TlsConfigurationRegistry tlsRegistry = Arc.container().select(TlsConfigurationRegistry.class).get();
        final Optional<String> maybeTlsConfigName = config.tlsConfigurationName();
        if (maybeTlsConfigName.isEmpty()) {
            if (config.keyStore().isPresent() || config.trustStore().isPresent()) {
                final String registryKey = "quarkus-cxf-client-" + configKey;
                final Optional<TlsConfiguration> cachedTlsConfiguration = tlsRegistry.get(registryKey);
                if (cachedTlsConfiguration.isPresent()) {
                    return cachedTlsConfiguration.get();
                }

                final KeyStoreOptionsBase keyStoreOptions;
                final KeyStore keyStore;
                if (config.keyStore().isPresent()) {
                    keyStoreOptions = keyStoreOptions(config.keyStoreType(),
                            "quarkus.cxf.client." + configKey + ".key-store-type");
                    keyStoreOptions
                            .setPassword(config.keyStorePassword().orElse(null))
                            .setValue(Buffer.buffer(CXFRuntimeUtils.read(config.keyStore().get())))
                            .setPath(config.keyStore().orElse(null));
                    if (config.keyPassword().isPresent()) {
                        keyStoreOptions.setAliasPassword(config.keyPassword().get());
                    }
                    try {
                        keyStore = keyStoreOptions.loadKeyStore(vertx);
                    } catch (Exception e) {
                        throw new RuntimeException("Could not load key store from " + config.keyStore().get(), e);
                    }
                } else {
                    keyStore = null;
                    keyStoreOptions = null;
                }

                final KeyStoreOptionsBase trustOptions;
                final KeyStore trustStore;
                if (config.trustStore().isPresent()) {
                    trustOptions = keyStoreOptions(config.trustStoreType(),
                            "quarkus.cxf.client." + configKey + ".trust-store-type");
                    trustOptions
                            .setPassword(config.trustStorePassword().orElse(null))
                            .setValue(Buffer.buffer(CXFRuntimeUtils.read(config.trustStore().get())))
                            .setPath(config.trustStore().orElse(null));
                    try {
                        trustStore = trustOptions.loadKeyStore(vertx);
                    } catch (Exception e) {
                        throw new RuntimeException("Could not load trust store from " + config.trustStore().get(), e);
                    }
                } else {
                    trustOptions = null;
                    trustStore = null;
                }

                final CxfTlsConfiguration cxfTlsConfiguration = new CxfTlsConfiguration(
                        keyStoreOptions,
                        keyStore,
                        trustOptions,
                        trustStore,
                        registryKey);
                tlsRegistry.register(registryKey, cxfTlsConfiguration);
                return cxfTlsConfiguration;
            } else {
                /* use global client tls configuration */
                Optional<TlsConfiguration> maybeTlsConfig = tlsRegistry.get(globalConfig.tlsConfigurationName());
                if (maybeTlsConfig.isPresent()) {
                    return maybeTlsConfig.get();
                } else {
                    throw new IllegalStateException(
                            "No such TLS configuration quarkus.tls." + globalConfig.tlsConfigurationName());
                }
            }
        } else {
            /* tls-configuration-name is set */

            if (config.keyStore().isPresent()) {
                throw new IllegalStateException("The configuration options"
                        + " quarkus.cxf.client." + configKey + ".tls-configuration-name"
                        + " and quarkus.cxf.client." + configKey + ".key-store cannot be both set at the same time."
                        + " Use one or the other way to set the TLS options.");
            }
            if (config.trustStore().isPresent()) {
                throw new IllegalStateException("The configuration options"
                        + " quarkus.cxf.client." + configKey + ".tls-configuration-name"
                        + " and quarkus.cxf.client." + configKey + ".trust-store cannot be both set at the same time."
                        + " Use one or the other way to set the TLS options.");
            }

            Optional<TlsConfiguration> maybeTlsConfig = tlsRegistry.get(maybeTlsConfigName.get());
            if (maybeTlsConfig.isPresent()) {
                return maybeTlsConfig.get();
            } else {
                throw new IllegalStateException("No such TLS configuration quarkus.tls." + maybeTlsConfigName.get());
            }
        }
    }

    private static KeyStoreOptionsBase keyStoreOptions(String type, String configKey) {
        return switch (type.toUpperCase(Locale.ROOT)) {
            case "JKS": {
                yield new JksOptions();
            }
            case "PKCS12": {
                yield new PfxOptions();
            }
            default:
                throw new IllegalArgumentException("Unexpected key store type " + type + " for " + configKey);
        };
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

    public String getProxyServer() {
        return proxyServer;
    }

    public Integer getProxyServerPort() {
        return proxyServerPort;
    }

    public String getNonProxyHosts() {
        return nonProxyHosts;
    }

    public ProxyServerType getProxyServerType() {
        return proxyServerType;
    }

    public String getProxyUsername() {
        return proxyUsername;
    }

    public String getProxyPassword() {
        return proxyPassword;
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
            return auth.username().or(config::username);
        }

        @Override
        public Optional<String> password() {
            return auth.password().or(config::password);
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
