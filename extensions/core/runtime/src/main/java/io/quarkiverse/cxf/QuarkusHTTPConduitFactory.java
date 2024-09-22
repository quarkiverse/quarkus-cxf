package io.quarkiverse.cxf;

import java.io.IOException;
import java.util.Optional;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import org.apache.cxf.Bus;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.configuration.security.ProxyAuthorizationPolicy;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HTTPConduitFactory;
import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.apache.cxf.transport.http.HttpClientHTTPConduit;
import org.apache.cxf.transport.http.URLConnectionHTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.jboss.logging.Logger;

import io.quarkiverse.cxf.CxfClientConfig.HTTPConduitImpl;
import io.quarkiverse.cxf.CxfClientConfig.WellKnownHostnameVerifier;
import io.quarkiverse.cxf.vertx.http.client.HttpClientPool;
import io.quarkiverse.cxf.vertx.http.client.VertxHttpClientHTTPConduit;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.runtime.VertxCertificateHolder;
import io.quarkus.tls.runtime.config.TlsBucketConfig;
import io.vertx.core.Vertx;
import io.vertx.core.net.KeyCertOptions;

/**
 * A HTTPConduitFactory with some client specific configuration, such as timeouts and SSL.
 *
 * @since 3.8.1
 */
public class QuarkusHTTPConduitFactory implements HTTPConduitFactory {
    static final Logger log = Logger.getLogger(QuarkusHTTPConduitFactory.class);
    /**
     * The name of the environment variable defining the conduit type to use for QuarkusCXFDefault.
     * This is only for testing while VertxHttpClientHTTPConduitFactory is not stable.
     */
    public static final String QUARKUS_CXF_DEFAULT_HTTP_CONDUIT_FACTORY = "QUARKUS_CXF_DEFAULT_HTTP_CONDUIT_FACTORY";
    static HTTPConduitImpl defaultHTTPConduitImpl;

    private final HttpClientPool httpClientPool;
    private final CxfFixedConfig cxFixedConfig;
    private final CXFClientInfo cxfClientInfo;
    private final HTTPConduitFactory busHTTPConduitFactory;
    private final AuthorizationPolicy authorizationPolicy;
    private final HTTPConduitImpl defaultHTTPConduitFactory;
    private final Vertx vertx;

    public QuarkusHTTPConduitFactory(
            HttpClientPool httpClientPool,
            CxfFixedConfig cxFixedConfig,
            CXFClientInfo cxfClientInfo,
            HTTPConduitFactory busHTTPConduitFactory,
            AuthorizationPolicy authorizationPolicy,
            Vertx vertx) {
        super();
        this.httpClientPool = httpClientPool;
        this.cxFixedConfig = cxFixedConfig;
        this.cxfClientInfo = cxfClientInfo;
        this.busHTTPConduitFactory = busHTTPConduitFactory;
        this.authorizationPolicy = authorizationPolicy;
        this.defaultHTTPConduitFactory = HTTPConduitImpl.findDefaultHTTPConduitImpl();
        this.vertx = vertx;
    }

    @Override
    public HTTPConduit createConduit(HTTPTransportFactory f, Bus b, EndpointInfo localInfo, EndpointReferenceType target)
            throws IOException {
        HTTPConduitImpl httpConduitImpl = cxfClientInfo.getHttpConduitImpl();
        if (httpConduitImpl == null) {
            httpConduitImpl = cxFixedConfig.httpConduitFactory().orElse(null);
        }
        if (httpConduitImpl == null
                && (CXFRecorder.isHc5Present())
                && busHTTPConduitFactory != null) {
            return configure(
                    busHTTPConduitFactory.createConduit(f, b, localInfo, target),
                    cxfClientInfo);
        }

        if (httpConduitImpl == null) {
            httpConduitImpl = HTTPConduitImpl.QuarkusCXFDefault;
        }

        final HTTPConduit result;
        switch (httpConduitImpl) {
            case CXFDefault: {
                /*
                 * Mimic what is done in org.apache.cxf.transport.http.HTTPTransportFactory.getConduit(EndpointInfo,
                 * EndpointReferenceType, Bus)
                 */
                if (Boolean.getBoolean("org.apache.cxf.transport.http.forceURLConnection")) {
                    result = new URLConnectionHTTPConduit(b, localInfo, target);
                } else {
                    result = new HttpClientHTTPConduit(b, localInfo, target);
                }
                break;
            }
            case QuarkusCXFDefault:
                switch (defaultHTTPConduitFactory) {
                    case VertxHttpClientHTTPConduitFactory: {
                        result = new VertxHttpClientHTTPConduit(b, localInfo, target, httpClientPool);
                        break;
                    }
                    case URLConnectionHTTPConduitFactory: {
                        result = new URLConnectionHTTPConduit(b, localInfo, target);
                        break;
                    }
                    case HttpClientHTTPConduitFactory: {
                        result = new HttpClientHTTPConduit(b, localInfo, target);
                        break;
                    }
                    default:
                        throw new IllegalStateException("Unexpected " + HTTPConduitImpl.class.getSimpleName() + " value: "
                                + defaultHTTPConduitFactory);
                }
                break;
            case VertxHttpClientHTTPConduitFactory: {
                result = new VertxHttpClientHTTPConduit(b, localInfo, target, httpClientPool);
                break;
            }
            case URLConnectionHTTPConduitFactory: {
                result = new URLConnectionHTTPConduit(b, localInfo, target);
                break;
            }
            case HttpClientHTTPConduitFactory: {
                result = new HttpClientHTTPConduit(b, localInfo, target);
                break;
            }
            default:
                throw new IllegalStateException("Unexpected " + HTTPConduitImpl.class.getSimpleName() + " value: "
                        + httpConduitImpl);
        }
        return configure(result, cxfClientInfo);
    }

    private HTTPConduit configure(HTTPConduit httpConduit, CXFClientInfo cxfClientInfo) throws IOException {
        final String hostnameVerifierName = cxfClientInfo.getHostnameVerifier();
        final TlsConfiguration tlsConfig = cxfClientInfo.getTlsConfiguration();
        if (hostnameVerifierName != null || tlsConfig != null) {
            TLSClientParameters tlsCP = new TLSClientParameters();

            if (hostnameVerifierName != null) {
                final Optional<WellKnownHostnameVerifier> wellKnownHostNameVerifierName = WellKnownHostnameVerifier
                        .of(hostnameVerifierName);
                if (wellKnownHostNameVerifierName.isPresent()) {
                    wellKnownHostNameVerifierName.get().configure(tlsCP);
                } else {
                    final HostnameVerifier hostnameVerifier = CXFRuntimeUtils.getInstance(hostnameVerifierName, true);
                    if (hostnameVerifier == null) {
                        throw new RuntimeException("Could not find or instantiate " + hostnameVerifierName);
                    }
                    tlsCP.setHostnameVerifier(hostnameVerifier);
                }
            }

            if (tlsConfig != null) {
                final KeyCertOptions keyStoreOptions = tlsConfig.getKeyStoreOptions();
                if (keyStoreOptions != null) {
                    try {
                        final KeyManagerFactory kmf = keyStoreOptions.getKeyManagerFactory(vertx);
                        tlsCP.setKeyManagers(kmf.getKeyManagers());
                    } catch (Exception e) {
                        throw new RuntimeException("Could not set up key manager factory", e);
                    }
                }

                if (tlsConfig.getTrustStoreOptions() != null) {
                    try {
                        final TrustManagerFactory tmf = tlsConfig.getTrustStoreOptions().getTrustManagerFactory(vertx);
                        tlsCP.setTrustManagers(tmf.getTrustManagers());
                    } catch (Exception e) {
                        throw new RuntimeException("Could not set up trust manager factory", e);
                    }
                }

                if (tlsConfig instanceof VertxCertificateHolder) {
                    final VertxCertificateHolder vertxCertificateHOlder = (VertxCertificateHolder) tlsConfig;
                    final TlsBucketConfig bucketConfig = vertxCertificateHOlder.config();
                    bucketConfig.cipherSuites().ifPresent(tlsCP::setCipherSuites);
                }
            }

            httpConduit.setTlsClientParameters(tlsCP);
        }

        final HTTPClientPolicy policy = new HTTPClientPolicy();
        httpConduit.setClient(policy);
        policy.setConnectionTimeout(cxfClientInfo.getConnectionTimeout());
        policy.setReceiveTimeout(cxfClientInfo.getReceiveTimeout());
        policy.setConnectionRequestTimeout(cxfClientInfo.getConnectionRequestTimeout());
        policy.setAutoRedirect(cxfClientInfo.isAutoRedirect());
        policy.setMaxRetransmits(cxfClientInfo.getMaxRetransmits());
        policy.setAllowChunking(cxfClientInfo.isAllowChunking());
        policy.setChunkingThreshold(cxfClientInfo.getChunkingThreshold());
        policy.setChunkLength(cxfClientInfo.getChunkLength());
        {
            final String value = cxfClientInfo.getAccept();
            if (value != null) {
                policy.setAccept(value);
            }
        }
        {
            final String value = cxfClientInfo.getAcceptLanguage();
            if (value != null) {
                policy.setAcceptLanguage(value);
            }
        }
        {
            final String value = cxfClientInfo.getAcceptEncoding();
            if (value != null) {
                policy.setAcceptEncoding(value);
            }
        }
        {
            final String value = cxfClientInfo.getContentType();
            if (value != null) {
                policy.setContentType(value);
            }
        }
        {
            final String value = cxfClientInfo.getHost();
            if (value != null) {
                policy.setHost(value);
            }
        }
        policy.setConnection(cxfClientInfo.getConnection());
        {
            final String value = cxfClientInfo.getCacheControl();
            if (value != null) {
                policy.setCacheControl(value);
            }
        }
        policy.setVersion(cxfClientInfo.getVersion());
        {
            final String value = cxfClientInfo.getBrowserType();
            if (value != null) {
                policy.setBrowserType(value);
            }
        }
        {
            final String value = cxfClientInfo.getDecoupledEndpoint();
            if (value != null) {
                policy.setDecoupledEndpoint(value);
            }
        }
        {
            final String value = cxfClientInfo.getProxyServer();
            if (value != null) {
                policy.setProxyServer(value);
            }
        }
        {
            final Integer value = cxfClientInfo.getProxyServerPort();
            if (value != null) {
                policy.setProxyServerPort(value);
            }
        }
        {
            final String value = cxfClientInfo.getNonProxyHosts();
            if (value != null) {
                policy.setNonProxyHosts(value);
            }
        }
        policy.setProxyServerType(cxfClientInfo.getProxyServerType());

        final String proxyUsername = cxfClientInfo.getProxyUsername();
        if (proxyUsername != null) {
            final String proxyPassword = cxfClientInfo.getProxyPassword();
            final ProxyAuthorizationPolicy proxyAuth = new ProxyAuthorizationPolicy();
            proxyAuth.setUserName(proxyUsername);
            proxyAuth.setPassword(proxyPassword);
            httpConduit.setProxyAuthorization(proxyAuth);
        }

        if (authorizationPolicy != null && cxfClientInfo.isSecureWsdlAccess()) {
            /*
             * This is the only way how the AuthorizationPolicy can be set early enough to be effective for the WSDL
             * GET request. We do not do it by default because of backwards compatibility and for the user to think
             * twice whether his WSDL URL uses HTTPS and only then enable secureWsdlAccess
             */
            httpConduit.setAuthorization(authorizationPolicy);
        }

        return httpConduit;
    }

}
