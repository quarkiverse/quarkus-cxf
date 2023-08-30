package io.quarkiverse.cxf;

import java.util.List;
import java.util.Optional;

import org.apache.cxf.transports.http.configuration.ConnectionType;
import org.apache.cxf.transports.http.configuration.ProxyServerType;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConvertWith;

/**
 * A class that provides configurable options of a CXF client.
 */
@ConfigGroup
public class CxfClientConfig {

    /**
     * The client WSDL path
     */
    @ConfigItem(name = "wsdl")
    public Optional<String> wsdlPath;

    /**
     * The URL of the SOAP Binding, should be one of four values:
     *
     * * `+http://schemas.xmlsoap.org/wsdl/soap/http+` for SOAP11HTTP_BINDING
     * * `+http://schemas.xmlsoap.org/wsdl/soap/http?mtom=true+` for SOAP11HTTP_MTOM_BINDING
     * * `+http://www.w3.org/2003/05/soap/bindings/HTTP/+` for SOAP12HTTP_BINDING
     * * `+http://www.w3.org/2003/05/soap/bindings/HTTP/?mtom=true+` for SOAP12HTTP_MTOM_BINDING
     *
     * @asciidoclet
     */
    @ConfigItem
    public Optional<String> soapBinding;

    /**
     * The client endpoint URL
     */
    @ConfigItem
    public Optional<String> clientEndpointUrl;

    /**
     * The client endpoint namespace
     */
    @ConfigItem
    public Optional<String> endpointNamespace;

    /**
     * The client endpoint name
     */
    @ConfigItem
    public Optional<String> endpointName;

    /**
     * The username for HTTP Basic auth
     */
    @ConfigItem
    public Optional<String> username;

    /**
     * The password for HTTP Basic auth
     */
    @ConfigItem
    public Optional<String> password;

    /**
     * A comma-separated list of fully qualified CXF Feature class names.
     * <p>
     * Example:
     *
     * <pre>
     * quarkus.cxf.endpoint.myClient.features = org.apache.cxf.ext.logging.LoggingFeature
     * </pre>
     * <p>
     * Note that the {@code LoggingFeature} is available through the <a href="../quarkus-cxf-rt-features-metrics.html">Logging
     * Feature</a> extension.
     */
    @ConfigItem
    public Optional<List<String>> features;

    /**
     * The comma-separated list of Handler classes
     */
    @ConfigItem
    public Optional<List<String>> handlers;

    /**
     * The comma-separated list of InInterceptor classes
     */
    @ConfigItem
    public Optional<List<String>> inInterceptors;

    /**
     * The comma-separated list of OutInterceptor classes
     */
    @ConfigItem
    public Optional<List<String>> outInterceptors;

    /**
     * The comma-separated list of OutFaultInterceptor classes
     */
    @ConfigItem
    public Optional<List<String>> outFaultInterceptors;

    /**
     * The comma-separated list of InFaultInterceptor classes
     */
    @ConfigItem
    public Optional<List<String>> inFaultInterceptors;

    /* org.apache.cxf.transports.http.configuration.HTTPClientPolicy attributes */
    /**
     * Specifies the amount of time, in milliseconds, that the consumer will attempt to establish a connection before it times
     * out. 0 is infinite.
     */
    @ConfigItem(defaultValue = "30000")
    public long connectionTimeout;
    /**
     * Specifies the amount of time, in milliseconds, that the consumer will wait for a response before it times out. 0 is
     * infinite.
     */
    @ConfigItem(defaultValue = "60000")
    public long receiveTimeout;
    /**
     * Specifies the amount of time, in milliseconds, used when requesting a connection from the connection manager(if
     * appliable). 0 is infinite.
     */
    @ConfigItem(defaultValue = "60000")
    public long connectionRequestTimeout;
    /**
     * Specifies if the consumer will automatically follow a server issued redirection.
     * (name is not part of standard)
     */
    @ConfigItem(defaultValue = "false")
    public boolean autoRedirect;
    /**
     * Specifies the maximum amount of retransmits that are allowed for redirects. Retransmits for
     * authorization is included in the retransmit count. Each redirect may cause another
     * retransmit for a UNAUTHORIZED response code, ie. 401.
     * Any negative number indicates unlimited retransmits,
     * although, loop protection is provided.
     * The default is unlimited.
     * (name is not part of standard)
     */
    @ConfigItem(defaultValue = "-1")
    public int maxRetransmits;
    /**
     * If true, the client is free to use chunking streams if it wants, but it is not
     * required to use chunking streams. If false, the client
     * must use regular, non-chunked requests in all cases.
     */
    @ConfigItem(defaultValue = "true")
    public boolean allowChunking;
    /**
     * If AllowChunking is true, this sets the threshold at which messages start
     * getting chunked. Messages under this limit do not get chunked.
     */
    @ConfigItem(defaultValue = "4096")
    public int chunkingThreshold;
    /**
     * Specifies the chunk length for a HttpURLConnection. This value is used in
     * java.net.HttpURLConnection.setChunkedStreamingMode(int chunklen). chunklen indicates the number of bytes to write in each
     * chunk. If chunklen is less than or equal to zero, a default value will be used.
     */
    @ConfigItem(defaultValue = "-1")
    public int chunkLength;
    /**
     * Specifies the MIME types the client is prepared to handle (e.g., HTML, JPEG, GIF, etc.)
     */
    @ConfigItem
    public Optional<String> accept;
    /**
     * Specifies the language the client desires (e.g., English, French, etc.)
     */
    @ConfigItem
    public Optional<String> acceptLanguage;
    /**
     * Specifies the encoding the client is prepared to handle (e.g., gzip)
     */
    @ConfigItem
    public Optional<String> acceptEncoding;
    /**
     * Specifies the content type of the stream being sent in a post request.
     * (this should be text/xml for web services, or can be set to
     * application/x-www-form-urlencoded if the client is sending form data).
     */
    @ConfigItem
    public Optional<String> contentType;
    /**
     * Specifies the Internet host and port number of the resource on which the request is being invoked.
     * This is sent by default based upon the URL. Certain DNS scenarios or
     * application designs may request you to set this, but typically it is
     * not required.
     */
    @ConfigItem
    public Optional<String> host;
    /**
     * The connection disposition. If close the connection to the server is closed
     * after each request/response dialog. If Keep-Alive the client requests the server
     * to keep the connection open, and if the server honors the keep alive request,
     * the connection is reused. Many servers and proxies do not honor keep-alive requests.
     *
     */
    @ConfigItem(defaultValue = "Keep-Alive")
    @ConvertWith(ConnectionTypeConverter.class)
    public ConnectionType connection;
    /**
     * Most commonly used to specify no-cache, however the standard supports a
     * dozen or so caching related directives for requests
     */
    @ConfigItem
    public Optional<String> cacheControl;
    /**
     * HTTP Version used for the connection. The "auto" default will use whatever the default is
     * for the HTTPConduit implementation.
     */
    @ConfigItem(defaultValue = "auto")
    public String version;
    /**
     * aka User-Agent
     * Specifies the type of browser is sending the request. This is usually only
     * needed when sites have HTML customized to Netscape vs IE, etc, but can
     * also be used to optimize for different SOAP stacks.
     */
    @ConfigItem
    protected Optional<String> browserType;
    /**
     * Specifies the URL of a decoupled endpoint for the receipt of responses over a separate provider->consumer connection.
     */
    @ConfigItem
    protected Optional<String> decoupledEndpoint;
    /**
     * Specifies the address of proxy server if one is used.
     */
    @ConfigItem
    protected Optional<String> proxyServer;
    /**
     * Specifies the port number used by the proxy server.
     */
    @ConfigItem
    protected Optional<Integer> proxyServerPort;
    /**
     * Specifies the list of hostnames that will not use the proxy configuration.
     * Examples of value:
     * * "localhost" -> A single hostname
     * * "localhost|www.google.com" -> 2 hostnames that will not use the proxy configuration
     * * "localhost|www.google.*|*.apache.org" -> It's also possible to use a pattern-like value
     */
    @ConfigItem
    protected Optional<String> nonProxyHosts;
    /**
     * Specifies the type of the proxy server. Can be either HTTP or SOCKS.
     */
    @ConfigItem(defaultValue = "HTTP")
    protected ProxyServerType proxyServerType;

}
