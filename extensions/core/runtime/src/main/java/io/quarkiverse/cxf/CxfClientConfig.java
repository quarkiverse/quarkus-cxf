package io.quarkiverse.cxf;

import java.util.List;
import java.util.Optional;

import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.transports.http.configuration.ConnectionType;
import org.apache.cxf.transports.http.configuration.ProxyServerType;

import io.quarkus.runtime.annotations.ConfigDocEnumValue;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * A class that provides configurable options of a CXF client.
 */
@ConfigGroup
public interface CxfClientConfig {

    /**
     * The client WSDL path
     */
    @WithName("wsdl")
    public Optional<String> wsdlPath();

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
    public Optional<String> soapBinding();

    /**
     * The client endpoint URL
     */
    public Optional<String> clientEndpointUrl();

    /**
     * The client endpoint namespace
     */
    public Optional<String> endpointNamespace();

    /**
     * The client endpoint name
     */
    public Optional<String> endpointName();

    /**
     * The username for HTTP Basic auth
     */
    public Optional<String> username();

    /**
     * The password for HTTP Basic auth
     */
    public Optional<String> password();

    /**
     * A comma-separated list of fully qualified CXF Feature class names.
     * <p>
     * Example:
     *
     * <pre>
     * quarkus.cxf.endpoint.myClient.features = org.apache.cxf.ext.logging.LoggingFeature
     * </pre>
     * <p>
     * Note that the {@code LoggingFeature} is available through the
     * <a href="../quarkus-cxf-rt-features-metrics.html">Logging
     * Feature</a> extension.
     */
    public Optional<List<String>> features();

    /**
     * The comma-separated list of Handler classes
     */
    public Optional<List<String>> handlers();

    /**
     * The comma-separated list of InInterceptor classes
     */
    public Optional<List<String>> inInterceptors();

    /**
     * The comma-separated list of OutInterceptor classes
     */
    public Optional<List<String>> outInterceptors();

    /**
     * The comma-separated list of OutFaultInterceptor classes
     */
    public Optional<List<String>> outFaultInterceptors();

    /**
     * The comma-separated list of InFaultInterceptor classes
     */
    public Optional<List<String>> inFaultInterceptors();

    /* org.apache.cxf.transports.http.configuration.HTTPClientPolicy attributes */
    /**
     * Specifies the amount of time, in milliseconds, that the consumer will attempt to establish a connection before it
     * times
     * out. 0 is infinite.
     */
    @WithDefault("30000")
    public long connectionTimeout();

    /**
     * Specifies the amount of time, in milliseconds, that the consumer will wait for a response before it times out. 0
     * is
     * infinite.
     */
    @WithDefault("60000")
    public long receiveTimeout();

    /**
     * Specifies the amount of time, in milliseconds, used when requesting a connection from the connection manager(if
     * appliable). 0 is infinite.
     */
    @WithDefault("60000")
    public long connectionRequestTimeout();

    /**
     * Specifies if the consumer will automatically follow a server issued redirection.
     * (name is not part of standard)
     */
    @WithDefault("false")
    public boolean autoRedirect();

    /**
     * Specifies the maximum amount of retransmits that are allowed for redirects. Retransmits for
     * authorization is included in the retransmit count. Each redirect may cause another
     * retransmit for a UNAUTHORIZED response code, ie. 401.
     * Any negative number indicates unlimited retransmits,
     * although, loop protection is provided.
     * The default is unlimited.
     * (name is not part of standard)
     */
    @WithDefault("-1")
    public int maxRetransmits();

    /**
     * If true, the client is free to use chunking streams if it wants, but it is not
     * required to use chunking streams. If false, the client
     * must use regular, non-chunked requests in all cases.
     */
    @WithDefault("true")
    public boolean allowChunking();

    /**
     * If AllowChunking is true, this sets the threshold at which messages start
     * getting chunked. Messages under this limit do not get chunked.
     */
    @WithDefault("4096")
    public int chunkingThreshold();

    /**
     * Specifies the chunk length for a HttpURLConnection. This value is used in
     * java.net.HttpURLConnection.setChunkedStreamingMode(int chunklen). chunklen indicates the number of bytes to write
     * in each
     * chunk. If chunklen is less than or equal to zero, a default value will be used.
     */
    @WithDefault("-1")
    public int chunkLength();

    /**
     * Specifies the MIME types the client is prepared to handle (e.g., HTML, JPEG, GIF, etc.)
     */
    public Optional<String> accept();

    /**
     * Specifies the language the client desires (e.g., English, French, etc.)
     */
    public Optional<String> acceptLanguage();

    /**
     * Specifies the encoding the client is prepared to handle (e.g., gzip)
     */
    public Optional<String> acceptEncoding();

    /**
     * Specifies the content type of the stream being sent in a post request.
     * (this should be text/xml for web services, or can be set to
     * application/x-www-form-urlencoded if the client is sending form data).
     */
    public Optional<String> contentType();

    /**
     * Specifies the Internet host and port number of the resource on which the request is being invoked.
     * This is sent by default based upon the URL. Certain DNS scenarios or
     * application designs may request you to set this, but typically it is
     * not required.
     */
    public Optional<String> host();

    /**
     * The connection disposition. If close the connection to the server is closed
     * after each request/response dialog. If Keep-Alive the client requests the server
     * to keep the connection open, and if the server honors the keep alive request,
     * the connection is reused. Many servers and proxies do not honor keep-alive requests.
     *
     */
    @WithDefault("Keep-Alive")
    @WithConverter(ConnectionTypeConverter.class)
    public ConnectionType connection();

    /**
     * Most commonly used to specify no-cache, however the standard supports a
     * dozen or so caching related directives for requests
     */
    public Optional<String> cacheControl();

    /**
     * HTTP Version used for the connection. The "auto" default will use whatever the default is
     * for the HTTPConduit implementation.
     */
    @WithDefault("auto")
    public String version();

    /**
     * aka User-Agent
     * Specifies the type of browser is sending the request. This is usually only
     * needed when sites have HTML customized to Netscape vs IE, etc, but can
     * also be used to optimize for different SOAP stacks.
     */
    public Optional<String> browserType();

    /**
     * Specifies the URL of a decoupled endpoint for the receipt of responses over a separate provider->consumer
     * connection.
     */
    public Optional<String> decoupledEndpoint();

    /**
     * Specifies the address of proxy server if one is used.
     */
    public Optional<String> proxyServer();

    /**
     * Specifies the port number used by the proxy server.
     */
    public Optional<Integer> proxyServerPort();

    /**
     * Specifies the list of hostnames that will not use the proxy configuration.
     * Examples of value:
     * * "localhost" -> A single hostname
     * * "localhost|www.google.com" -> 2 hostnames that will not use the proxy configuration
     * * "localhost|www.google.*|*.apache.org" -> It's also possible to use a pattern-like value
     */
    public Optional<String> nonProxyHosts();

    /**
     * Specifies the type of the proxy server. Can be either HTTP or SOCKS.
     */
    @WithDefault("HTTP")
    public ProxyServerType proxyServerType();

    /**
     * Username for the proxy authentication
     */
    public Optional<String> proxyUsername();

    /**
     * Password for the proxy authentication
     */
    public Optional<String> proxyPassword();

    /**
     * Select the {@code HTTPConduitFactory} implementation for this client.
     * <ul>
     * <li>{@code QuarkusCXFDefault} (default): if {@code io.quarkiverse.cxf:quarkus-cxf-rt-transports-http-hc5} is
     * present in class path, then its {@code HTTPConduitFactory} implementation will be used;
     * otherwise this value is equivalent with {@code URLConnectionHTTPConduitFactory} (this may change, once
     * issue <a href="https://github.com/quarkiverse/quarkus-cxf/issues/992">#992</a> gets resolved in CXF)
     * <li>{@code CXFDefault}: the selection of {@code HTTPConduitFactory} implementation is left to CXF
     * <li>{@code HttpClientHTTPConduitFactory}: the {@code HTTPConduitFactory} for this client will be set to an
     * implementation always returning {@code org.apache.cxf.transport.http.HttpClientHTTPConduit}. This will use
     * {@code java.net.http.HttpClient} as the underlying HTTP client.
     * <li>{@code URLConnectionHTTPConduitFactory}: the {@code HTTPConduitFactory} for this client will be set to an
     * implementation always returning {@code org.apache.cxf.transport.http.URLConnectionHTTPConduit}. This will use
     * {@code java.net.HttpURLConnection} as the underlying HTTP client.
     * </ul>
     */
    public Optional<HTTPConduitImpl> httpConduitFactory();

    /**
     * The trust store location for this client. The resource is first looked up in the classpath, then in the file
     * system.
     */
    public Optional<String> trustStore();

    /**
     * The trust store password
     */
    public Optional<String> trustStorePassword();

    /**
     * The type of the trust store.
     */
    @WithDefault("JKS")
    public String trustStoreType();

    /**
     * Can be one of the following:
     * <ul>
     * <li>One of the well known values: {@code AllowAllHostnameVerifier},
     * {@code HttpsURLConnectionDefaultHostnameVerifier}
     * <li>A fully qualified class name implementing {@code javax.net.ssl.HostnameVerifier}. The class must have a public
     * no-argument constructor.
     * <li>A bean name prefixed with {@code #} that will be looked up in the CDI container; example:
     * {@code #myHostnameVerifier}
     * </ul>
     * If not specified, then the creation of the {@code HostnameVerifier} is delegated to CXF, which boils down to
     * {@code org.apache.cxf.transport.https.httpclient.DefaultHostnameVerifier} with the default
     * {@code org.apache.cxf.transport.https.httpclient.PublicSuffixMatcherLoader}
     * as returned from {@code PublicSuffixMatcherLoader.getDefault()}.
     */
    public Optional<String> hostnameVerifier();

    public enum HTTPConduitImpl {
        @ConfigDocEnumValue("QuarkusCXFDefault")
        QuarkusCXFDefault,
        @ConfigDocEnumValue("CXFDefault")
        CXFDefault,
        @ConfigDocEnumValue("HttpClientHTTPConduitFactory")
        HttpClientHTTPConduitFactory,
        @ConfigDocEnumValue("URLConnectionHTTPConduitFactory")
        URLConnectionHTTPConduitFactory;

        public static HTTPConduitImpl fromOptional(Optional<HTTPConduitImpl> optional, boolean hc5Present, String key) {
            if (optional.isPresent()
                    && optional.get() != HTTPConduitImpl.CXFDefault
                    && hc5Present) {
                /*
                 * This is bad: the user has to choose whether he wants URLConnectionHTTPConduitFactory or
                 * QuarkusAsyncHTTPConduitFactory
                 */
                throw new RuntimeException(
                        "You cannot use " + key + "=" + optional.get().name()
                                + " and io.quarkiverse.cxf:quarkus-cxf-rt-transports-http-hc5 simultaneously."
                                + " Either remove " + key + "=" + optional.get().name() + " from application.properties"
                                + " or remove the io.quarkiverse.cxf:quarkus-cxf-rt-transports-http-hc5 dependency");
            } else if (!optional.isPresent() && hc5Present) {
                return HTTPConduitImpl.CXFDefault;
            } else {
                return optional.orElse(HTTPConduitImpl.QuarkusCXFDefault);
            }
        }
    }

    public enum WellKnownHostnameVerifier {

        AllowAllHostnameVerifier {
            @Override
            public void configure(TLSClientParameters params) {
                params.setDisableCNCheck(true);
            }

        },
        HttpsURLConnectionDefaultHostnameVerifier {
            @Override
            public void configure(TLSClientParameters params) {
                params.setUseHttpsURLConnectionDefaultHostnameVerifier(true);
            }
        };

        public abstract void configure(TLSClientParameters params);

        public static Optional<WellKnownHostnameVerifier> of(String name) {
            if (name == null) {
                return Optional.empty();
            }
            for (WellKnownHostnameVerifier item : values()) {
                if (item.name().equals(name)) {
                    return Optional.of(item);
                }
            }
            return Optional.empty();
        }
    }

}
