package io.quarkiverse.cxf;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.cxf.transports.http.configuration.ConnectionType;
import org.apache.cxf.transports.http.configuration.ProxyServerType;

import io.quarkiverse.cxf.CxfClientConfig.HTTPConduitImpl;
import io.quarkus.arc.Unremovable;

/**
 * CXF client metadata - the complete set as known at runtime.
 */
@Unremovable
public class CXFClientInfo {
    private static final String DEFAULT_EP_ADDR = "http://localhost:8080";

    private final String sei;
    private final String endpointAddress;
    private final String wsdlUrl;
    private final String soapBinding;
    private final String wsNamespace;
    private final String wsName;
    private final String epNamespace;
    private final String epName;
    private final String username;
    private final String password;
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
     * Specifies the maximum amount of retransmits that are allowed for redirects. Retransmits for
     * authorization is included in the retransmit count. Each redirect may cause another
     * retransmit for a UNAUTHORIZED response code, ie. 401.
     * Any negative number indicates unlimited retransmits,
     * although, loop protection is provided.
     * The default is unlimited.
     * (name is not part of standard)
     */
    private final int maxRetransmits;
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

    /**
     * The trust store location. Can point to either a classpath resource or a file.
     */
    private final String trustStore;

    /**
     * The trust store password.
     */
    private final String trustStorePassword;

    /**
     * The type of the trust store. Defaults to "JKS".
     */
    private final String trustStoreType;
    private final String hostnameVerifier;

    private final HTTPConduitImpl httpConduitImpl;

    private final String configKey;

    public CXFClientInfo(CXFClientData other, CxfClientConfig config, String configKey) {
        this.sei = other.getSei();
        this.soapBinding = config.soapBinding().orElse(other.getSoapBinding());
        this.wsName = other.getWsName();
        this.wsNamespace = other.getWsNamespace();
        this.proxyClassRuntimeInitialized = other.isProxyClassRuntimeInitialized();
        Objects.requireNonNull(config);
        this.epNamespace = config.endpointNamespace().orElse(null);
        this.epName = config.endpointName().orElse(null);
        this.username = config.username().orElse(null);
        this.password = config.password().orElse(null);
        this.endpointAddress = config.clientEndpointUrl().orElse(DEFAULT_EP_ADDR + "/" + this.sei.toLowerCase());
        this.wsdlUrl = config.wsdlPath().orElse(null);
        addFeatures(config);
        addHandlers(config);
        addInterceptors(config);
        this.connectionTimeout = config.connectionTimeout();
        this.receiveTimeout = config.receiveTimeout();
        this.connectionRequestTimeout = config.connectionRequestTimeout();
        this.autoRedirect = config.autoRedirect();
        this.maxRetransmits = config.maxRetransmits();
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
        this.proxyServer = config.proxyServer().orElse(null);
        this.proxyServerPort = config.proxyServerPort().orElse(null);
        this.nonProxyHosts = config.nonProxyHosts().orElse(null);
        this.proxyServerType = config.proxyServerType();
        this.proxyUsername = config.proxyUsername().orElse(null);
        this.proxyPassword = config.proxyPassword().orElse(null);

        this.trustStore = config.trustStore().orElse(null);
        this.trustStorePassword = config.trustStorePassword().orElse(null);
        this.trustStoreType = Objects.requireNonNull(config.trustStoreType(), "trustStoreType cannot be null");
        this.hostnameVerifier = config.hostnameVerifier().orElse(null);

        this.httpConduitImpl = HTTPConduitImpl.fromOptional(config.httpConduitFactory(), CXFRecorder.isHc5Present(),
                "quarkus.cxf.client." + configKey + ".http-conduit-impl");
        this.configKey = configKey;
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

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
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

    public int getMaxRetransmits() {
        return maxRetransmits;
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

    public String getTrustStore() {
        return trustStore;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public String getTrustStoreType() {
        return trustStoreType;
    }

    public String getConfigKey() {
        return configKey;
    }

}
