package io.quarkiverse.cxf;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.cxf.Bus;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.configuration.security.ProxyAuthorizationPolicy;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HTTPConduitFactory;
import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.apache.cxf.transport.http.HttpClientHTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.transports.http.configuration.ProxyServerType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.jboss.logging.Logger;

import io.quarkiverse.cxf.vertx.http.client.HttpClientPool;
import io.quarkus.logging.Log;
import io.quarkus.proxy.ProxyConfiguration;
import io.quarkus.proxy.ProxyType;
import io.vertx.core.Vertx;

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

    private final CxfFixedConfig cxFixedConfig;
    private final CXFClientInfo cxfClientInfo;
    private final AuthorizationPolicy authorizationPolicy;
    private final Vertx vertx;
    private final HttpClientPool httpClientPool;

    public QuarkusHTTPConduitFactory(
            CxfFixedConfig cxFixedConfig,
            CXFClientInfo cxfClientInfo,
            AuthorizationPolicy authorizationPolicy,
            Vertx vertx,
            HttpClientPool httpClientPool) {
        super();
        this.cxFixedConfig = cxFixedConfig;
        this.cxfClientInfo = cxfClientInfo;
        this.authorizationPolicy = authorizationPolicy;
        this.vertx = vertx;
        this.httpClientPool = httpClientPool;
    }

    @Override
    public HTTPConduit createConduit(HTTPTransportFactory f, Bus b, EndpointInfo localInfo, EndpointReferenceType target)
            throws IOException {
        HTTPConduitSpec httpConduitImpl = cxfClientInfo.getHttpConduitImpl();
        if (httpConduitImpl == null) {
            httpConduitImpl = cxFixedConfig.httpConduitFactory().orElse(null);
        }

        if (httpConduitImpl == null) {
            httpConduitImpl = HTTPConduitImpl.QuarkusCXFDefault;
        }
        return configure(httpConduitImpl.resolveDefault(), cxfClientInfo, b, localInfo, target);
    }

    private HTTPConduit configure(HTTPConduitSpec httpConduitImpl, CXFClientInfo cxfClientInfo, Bus b,
            EndpointInfo localInfo,
            EndpointReferenceType target) throws IOException {
        final ProxyConfiguration pConfig = cxfClientInfo.getProxyConfiguration();
        final HTTPConduit httpConduit = httpConduitImpl.createConduit(
                cxfClientInfo,
                httpClientPool,
                b,
                localInfo,
                target,
                pConfig);
        if (httpConduit instanceof HttpClientHTTPConduit) {
            Log.warnf("Usage of %s is deprecated since Quarkus CXF 3.18.0."
                    + " You may want to review the options quarkus.cxf.http-conduit-factory and/or quarkus.cxf.client.\"%s\".http-conduit-factory",
                    HttpClientHTTPConduit.class.getName(),
                    cxfClientInfo.getConfigKey());
        }
        httpConduitImpl.tlsClientParameters(cxfClientInfo, vertx).ifPresent(httpConduit::setTlsClientParameters);
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

        if (pConfig != null && !"none".equals(pConfig.host())) {
            /*
             * We set the proxy settings on the HTTPClientPolicy only for the URLConnectionHTTPConduit.
             * VertxHttpClientHTTPConduit does not need it, because we pass the ProxyConfiguration to it directly.
             */
            policy.setProxyServer(pConfig.host());
            policy.setProxyServerPort(pConfig.port());
            toProxyType(pConfig.type()).ifPresent(policy::setProxyServerType);
            pConfig.nonProxyHosts().map(nph -> nph.stream().collect(Collectors.joining("|")))
                    .ifPresent(policy::setNonProxyHosts);

            final String proxyUsername = pConfig.username().orElse(null);
            if (proxyUsername != null) {
                final ProxyAuthorizationPolicy proxyAuth = new ProxyAuthorizationPolicy();
                proxyAuth.setUserName(proxyUsername);
                pConfig.password().ifPresent(proxyAuth::setPassword);
                httpConduit.setProxyAuthorization(proxyAuth);
            }
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

    static Optional<ProxyServerType> toProxyType(ProxyType proxyType) {
        switch (proxyType) {
            case HTTP:
                return Optional.of(ProxyServerType.HTTP);
            case SOCKS4:
                return Optional.of(ProxyServerType.SOCKS);
            default:
                return Optional.empty();
        }
    }

}
