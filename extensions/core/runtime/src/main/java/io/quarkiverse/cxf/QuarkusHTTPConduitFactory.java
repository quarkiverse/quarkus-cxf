package io.quarkiverse.cxf;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
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

/**
 * A HTTPConduitFactory with some client specific configuration, such as timeouts and SSL.
 *
 * @since 3.8.1
 */
public class QuarkusHTTPConduitFactory implements HTTPConduitFactory {
    private static final Logger log = Logger.getLogger(QuarkusHTTPConduitFactory.class);
    private final HttpClientPool httpClientPool;
    private final CxfFixedConfig cxFixedConfig;
    private final CXFClientInfo cxfClientInfo;
    private final HTTPConduitFactory busHTTPConduitFactory;
    private final AuthorizationPolicy authorizationPolicy;

    public QuarkusHTTPConduitFactory(
            HttpClientPool httpClientPool,
            CxfFixedConfig cxFixedConfig,
            CXFClientInfo cxfClientInfo,
            HTTPConduitFactory busHTTPConduitFactory,
            AuthorizationPolicy authorizationPolicy) {
        super();
        this.httpClientPool = httpClientPool;
        this.cxFixedConfig = cxFixedConfig;
        this.cxfClientInfo = cxfClientInfo;
        this.busHTTPConduitFactory = busHTTPConduitFactory;
        this.authorizationPolicy = authorizationPolicy;
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
        final String keyStorePath = cxfClientInfo.getKeyStore();
        final String trustStorePath = cxfClientInfo.getTrustStore();
        if (hostnameVerifierName != null || keyStorePath != null || trustStorePath != null) {
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

            if (keyStorePath != null) {
                final KeyStore keyStore;
                final KeyManagerFactory kmf;
                try (InputStream is = openStream(keyStorePath)) {
                    keyStore = KeyStore.getInstance(cxfClientInfo.getKeyStoreType());
                    final String pwd = cxfClientInfo.getKeyStorePassword();
                    keyStore.load(is, pwd == null ? null : pwd.toCharArray());
                    kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                    final String keyPassword = cxfClientInfo.getKeyPassword();
                    kmf.init(keyStore, (keyPassword != null) ? keyPassword.toCharArray() : null);
                } catch (IOException | NoSuchAlgorithmException | CertificateException | KeyStoreException
                        | UnrecoverableKeyException e) {
                    throw new RuntimeException("Could not load " + keyStorePath + " from class path or filesystem", e);
                }
                tlsCP.setKeyManagers(kmf.getKeyManagers());
            }

            if (trustStorePath != null) {
                final KeyStore trustStore;
                final TrustManagerFactory tmf;
                try (InputStream is = openStream(trustStorePath)) {
                    trustStore = KeyStore.getInstance(cxfClientInfo.getTrustStoreType());
                    final String pwd = cxfClientInfo.getTrustStorePassword();
                    trustStore.load(is, pwd == null ? null : pwd.toCharArray());
                    tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                    tmf.init(trustStore);
                } catch (IOException | NoSuchAlgorithmException | CertificateException | KeyStoreException e) {
                    throw new RuntimeException("Could not load " + trustStorePath + " from class path or filesystem", e);
                }
                tlsCP.setTrustManagers(tmf.getTrustManagers());
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

    private InputStream openStream(final String keystorePath) throws IOException {
        final URL url = Thread.currentThread().getContextClassLoader().getResource(keystorePath);
        if (url != null) {
            try {
                return url.openStream();
            } catch (IOException e) {
                throw new RuntimeException("Could not open " + keystorePath + " from the class path", e);
            }
        }
        final Path path = Path.of(keystorePath);
        if (Files.exists(path)) {
            try {
                return Files.newInputStream(path);
            } catch (IOException e) {
                throw new RuntimeException("Could not open " + keystorePath + " from the filesystem", e);
            }
        }
        final String msg = "Resource " + keystorePath + " exists neither in class path nor in the filesystem";
        log.error(msg);
        throw new IllegalStateException(msg);
    }

}
