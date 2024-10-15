package io.quarkiverse.cxf;

import java.io.IOException;

import org.apache.cxf.Bus;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.configuration.security.ProxyAuthorizationPolicy;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HTTPConduitFactory;
import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.jboss.logging.Logger;

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
    private final HTTPConduitSpec busHTTPConduitImpl;
    private final AuthorizationPolicy authorizationPolicy;
    private final Vertx vertx;

    public QuarkusHTTPConduitFactory(
            CxfFixedConfig cxFixedConfig,
            CXFClientInfo cxfClientInfo,
            HTTPConduitSpec busHTTPConduitImpl,
            AuthorizationPolicy authorizationPolicy,
            Vertx vertx) {
        super();
        this.cxFixedConfig = cxFixedConfig;
        this.cxfClientInfo = cxfClientInfo;
        this.busHTTPConduitImpl = busHTTPConduitImpl;
        this.authorizationPolicy = authorizationPolicy;
        this.vertx = vertx;
    }

    @Override
    public HTTPConduit createConduit(HTTPTransportFactory f, Bus b, EndpointInfo localInfo, EndpointReferenceType target)
            throws IOException {
        HTTPConduitSpec httpConduitImpl = cxfClientInfo.getHttpConduitImpl();
        if (httpConduitImpl == null) {
            httpConduitImpl = cxFixedConfig.httpConduitFactory().orElse(null);
        }
        if (httpConduitImpl == null
                && (CXFRecorder.isHc5Present())
                && busHTTPConduitImpl != null) {
            return configure(f, busHTTPConduitImpl.resolveDefault(), cxfClientInfo, b, localInfo, target);
        }

        if (httpConduitImpl == null) {
            httpConduitImpl = HTTPConduitImpl.QuarkusCXFDefault;
        }
        return configure(f, httpConduitImpl.resolveDefault(), cxfClientInfo, b, localInfo, target);
    }

    private HTTPConduit configure(HTTPTransportFactory f, HTTPConduitSpec httpConduitImpl, CXFClientInfo cxfClientInfo, Bus b,
            EndpointInfo localInfo,
            EndpointReferenceType target) throws IOException {
        final HTTPConduit httpConduit = httpConduitImpl.createConduit(f, b, localInfo, target);
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
