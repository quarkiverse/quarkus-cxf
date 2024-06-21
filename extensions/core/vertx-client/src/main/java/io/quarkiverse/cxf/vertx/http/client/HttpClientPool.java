package io.quarkiverse.cxf.vertx.http.client;

import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.Provider;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.net.ssl.ExtendedSSLSession;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.KeyManagerFactorySpi;
import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.StandardConstants;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.TrustManagerFactorySpi;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.jsse.SSLUtils;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.https.httpclient.DefaultHostnameVerifier;
import org.apache.cxf.transport.https.httpclient.PublicSuffixMatcherLoader;

import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslProvider;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.SSLEngineOptions;
import io.vertx.core.spi.tls.DefaultSslContextFactory;
import io.vertx.core.spi.tls.SslContextFactory;

public class HttpClientPool {
    private final Map<ClientSpec, HttpClient> clients = new ConcurrentHashMap<>();
    private final Vertx vertx;

    public HttpClientPool(Vertx vertx) {
        super();
        this.vertx = vertx;
    }

    public HttpClient getClient(ClientSpec spec) {
        return clients.computeIfAbsent(spec, v -> {
            final HttpClientOptions opts = new HttpClientOptions()
                    .setProtocolVersion(spec.getVersion());
            if (spec.isSsl()) {
                opts
                        .setSsl(true)
                        .setTrustAll(true)
                        .setSslEngineOptions(spec.createSslEngineOptions());
            }
            return vertx.createHttpClient(opts);
        });
    }

    public static class ClientSpec {
        protected static final Logger LOG = LogUtils.getL7dLogger(HTTPConduit.class);
        private final HttpVersion httpVersion;
        private final KeyManager[] keyManagers;
        private final TrustManager[] trustManagers;
        private final Set<String> cipherSuites;
        private final HostnameVerifier hostNameVerifier;

        private final int hashCode;
        private final boolean ssl;

        public ClientSpec(
                HttpVersion version,
                TLSClientParameters params) {
            this.httpVersion = version;
            int h = 31 + httpVersion.hashCode();
            if (params != null) {
                KeyManager[] kms = params.getKeyManagers();
                if (kms == null) {
                    kms = SSLUtils.getDefaultKeyStoreManagers(LOG);
                }
                try {
                    kms = org.apache.cxf.transport.https.SSLUtils.configureKeyManagersWithCertAlias(params, kms);
                } catch (GeneralSecurityException e) {
                    throw new VertxHttpException(e);
                }
                this.keyManagers = kms;

                TrustManager[] tms = params.getTrustManagers();
                if (tms == null) {
                    tms = SSLUtils.getDefaultTrustStoreManagers(LOG);
                }
                this.hostNameVerifier = getHostnameVerifier((TLSClientParameters) params);
                this.trustManagers = Stream.of(tms)
                        .map(t -> new X509TrustManagerWrapper((X509ExtendedTrustManager) t, hostNameVerifier))
                        .toArray(size -> new TrustManager[size]);

                String[] css;
                try {
                    css = SSLUtils.getCiphersuitesToInclude(
                            params.getCipherSuites(),
                            params.getCipherSuitesFilter(),
                            SSLContext.getDefault().getDefaultSSLParameters().getCipherSuites(),
                            Http2SecurityUtil.CIPHERS.toArray(new String[] {}),
                            LOG);
                } catch (NoSuchAlgorithmException e) {
                    throw new VertxHttpException(e);
                }
                this.cipherSuites = new LinkedHashSet<>(Arrays.asList(css));

                h = 31 * h + Arrays.hashCode(keyManagers);
                h = 31 * h + Arrays.hashCode(tms);
                h = 31 * h + hostNameVerifier.hashCode();
                h = 31 * h + cipherSuites.hashCode();
                this.ssl = true;
            } else {
                this.keyManagers = null;
                this.trustManagers = null;
                this.hostNameVerifier = null;
                this.cipherSuites = null;
                this.ssl = false;
            }

            this.hashCode = h;
        }

        public HttpVersion getVersion() {
            return httpVersion;
        }

        public SSLEngineOptions createSslEngineOptions() {
            return new CxfSSLEngineOptions(new CxfSslContextFactory(toSslContex()));
        }

        SslContext toSslContex() {

            final SslContextFactory builder = new DefaultSslContextFactory(SslProvider.JDK, true)
                    .forClient(true);

            try {
                return builder
                        // .applicationProtocols(Arrays.asList(params.getApplicationProtocols()))
                        .enabledCipherSuites(cipherSuites)
                        // .serverName(null)
                        .useAlpn(false)
                        .trustManagerFactory(new CxfTrustManagerFactory(trustManagers))
                        .keyMananagerFactory(new CxfKeyManagerFactory(keyManagers))
                        .clientAuth(ClientAuth.REQUIRE) // TODO do we need to make this configurable?
                        .create();
            } catch (SSLException e) {
                throw new VertxHttpException(e);
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ClientSpec other = (ClientSpec) obj;
            return httpVersion == other.httpVersion
                    && Objects.equals(cipherSuites, other.cipherSuites)
                    && Arrays.equals(keyManagers, other.keyManagers)
                    && Arrays.equals(trustManagers, other.trustManagers);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        public boolean isSsl() {
            return ssl;
        }

        static HostnameVerifier getHostnameVerifier(TLSClientParameters tlsClientParameters) {
            if (tlsClientParameters.getHostnameVerifier() != null) {
                return tlsClientParameters.getHostnameVerifier();
            } else if (tlsClientParameters.isUseHttpsURLConnectionDefaultHostnameVerifier()) {
                return HttpsURLConnection.getDefaultHostnameVerifier();
            } else if (tlsClientParameters.isDisableCNCheck()) {
                return ALLOW_ALL_HOSTNAME_VERIFIER;
            } else {
                return DEFAULT_HOSTNAME_VERIFIER;
            }
        }

        private static final HostnameVerifier DEFAULT_HOSTNAME_VERIFIER = new DefaultHostnameVerifier(
                PublicSuffixMatcherLoader.getDefault());
        private static final HostnameVerifier ALLOW_ALL_HOSTNAME_VERIFIER;
        static {
            final TLSClientParameters params = new TLSClientParameters();
            params.setDisableCNCheck(true);
            ALLOW_ALL_HOSTNAME_VERIFIER = org.apache.cxf.transport.https.SSLUtils.getHostnameVerifier(params);
        }

    }

    public record CxfSslContextFactory(SslContext create) implements SslContextFactory {
    }

    public static class CxfSSLEngineOptions extends SSLEngineOptions {

        private final SslContextFactory sslContextFactory;

        public CxfSSLEngineOptions(SslContextFactory sslContextFactory) {
            super();
            this.sslContextFactory = sslContextFactory;
        }

        @Override
        public SSLEngineOptions copy() {
            return new CxfSSLEngineOptions(sslContextFactory);
        }

        @Override
        public SslContextFactory sslContextFactory() {
            return sslContextFactory;
        }

    }

    private static final Provider PROVIDER = new Provider("", "0.0", "") {
    };

    static class CxfTrustManagerFactory extends TrustManagerFactory {

        CxfTrustManagerFactory(TrustManager... tm) {
            super(new TrustManagerFactorySpi() {
                @Override
                protected void engineInit(KeyStore keyStore) throws KeyStoreException {
                }

                @Override
                protected void engineInit(ManagerFactoryParameters managerFactoryParameters) {
                }

                @Override
                protected TrustManager[] engineGetTrustManagers() {
                    return tm;
                }
            }, PROVIDER, "");
        }
    }

    /*
     * The classes below are used by the HttpClient implementation to allow use of the
     * HostNameVerifier that is configured. HttpClient does not provide a hook or
     * anything to call into the HostNameVerifier after the certs are verified. It
     * prefers that the Hostname is verified at the same time as the certificates
     * but the only option for hostname is the global on/off system property. Thus,
     * we have to provide a X509TrustManagerWrapper that would turn off the
     * EndpointIdentificationAlgorithm and then handle the hostname verification
     * directly. However, since the peer certs are not yet verified, we also need to wrapper
     * the session so the HostnameVerifier things they are.
     */
    static class X509TrustManagerWrapper extends X509ExtendedTrustManager {

        private final X509TrustManager delegate;
        private final X509ExtendedTrustManager extendedDelegate;
        private final HostnameVerifier verifier;

        X509TrustManagerWrapper(X509TrustManager delegate, HostnameVerifier hnv) {
            this.delegate = delegate;
            this.verifier = hnv;
            this.extendedDelegate = delegate instanceof X509ExtendedTrustManager
                    ? (X509ExtendedTrustManager) delegate
                    : null;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String s) throws CertificateException {
            delegate.checkClientTrusted(chain, s);
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String s, Socket socket)
                throws CertificateException {
            if (extendedDelegate != null) {
                extendedDelegate.checkClientTrusted(chain, s, socket);
            } else {
                delegate.checkClientTrusted(chain, s);
            }
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String s, SSLEngine sslEngine)
                throws CertificateException {
            if (extendedDelegate != null) {
                extendedDelegate.checkClientTrusted(chain, s, sslEngine);
            } else {
                delegate.checkClientTrusted(chain, s);
            }
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String s) throws CertificateException {
            System.out.println("cst1: " + s);
            delegate.checkServerTrusted(chain, s);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String s, Socket socket)
                throws CertificateException {
            System.out.println("cst2: " + s);
            if (extendedDelegate != null) {
                extendedDelegate.checkServerTrusted(chain, s, socket);
            } else {
                delegate.checkServerTrusted(chain, s);
            }
        }

        private String getHostName(List<SNIServerName> names) {
            if (names == null) {
                return null;
            }
            for (SNIServerName n : names) {
                if (n.getType() != StandardConstants.SNI_HOST_NAME) {
                    continue;
                }
                if (n instanceof SNIHostName) {
                    SNIHostName hostname = (SNIHostName) n;
                    return hostname.getAsciiName();
                }
            }
            return null;
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String s, SSLEngine engine)
                throws CertificateException {
            if (extendedDelegate != null) {
                extendedDelegate.checkServerTrusted(chain, s, new SSLEngineWrapper(engine));
                //certificates are valid, now check hostnames
                SSLSession session = engine.getHandshakeSession();
                List<SNIServerName> names = null;
                if (session instanceof ExtendedSSLSession) {
                    ExtendedSSLSession extSession = (ExtendedSSLSession) session;
                    names = extSession.getRequestedServerNames();
                }

                boolean identifiable = false;
                String peerHost = session.getPeerHost();
                String hostname = getHostName(names);
                session = new SSLSessionWrapper(session, chain);
                if (hostname != null && verifier.verify(hostname, session)) {
                    identifiable = true;
                }
                if (!identifiable && !verifier.verify(peerHost, session)) {
                    throw new CertificateException(
                            "The https URL hostname " + peerHost + " does not match the "
                                    + "Common Name (CN) on the server certificate in the client's truststore. "
                                    + "Make sure server certificate is correct, or to disable this check "
                                    + "(NOT recommended for production) set the CXF client TLS "
                                    + "configuration property \"disableCNCheck\" to true.");
                }
            } else {
                delegate.checkServerTrusted(chain, s);
            }
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return delegate.getAcceptedIssuers();
        }
    }

    static class SSLEngineWrapper extends SSLEngine {
        final SSLEngine delegate;

        SSLEngineWrapper(SSLEngine delegate) {
            this.delegate = delegate;
        }

        @Override
        public SSLParameters getSSLParameters() {
            //make sure the hostname verification is not done in the default X509 stuff
            //so we can do it later
            SSLParameters params = delegate.getSSLParameters();
            params.setEndpointIdentificationAlgorithm(null);
            return params;
        }

        @Override
        public SSLSession getHandshakeSession() {
            return delegate.getHandshakeSession();
        }

        @Override
        public void beginHandshake() throws SSLException {
            delegate.beginHandshake();
        }

        @Override
        public void closeInbound() throws SSLException {
            delegate.closeInbound();
        }

        @Override
        public void closeOutbound() {
            delegate.closeOutbound();
        }

        @Override
        public Runnable getDelegatedTask() {
            return delegate.getDelegatedTask();
        }

        @Override
        public boolean getEnableSessionCreation() {
            return delegate.getEnableSessionCreation();
        }

        @Override
        public String[] getEnabledCipherSuites() {
            return delegate.getEnabledCipherSuites();
        }

        @Override
        public String[] getEnabledProtocols() {
            return delegate.getEnabledProtocols();
        }

        @Override
        public HandshakeStatus getHandshakeStatus() {
            return delegate.getHandshakeStatus();
        }

        @Override
        public boolean getNeedClientAuth() {
            return delegate.getNeedClientAuth();
        }

        @Override
        public SSLSession getSession() {
            return delegate.getSession();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return delegate.getSupportedCipherSuites();
        }

        @Override
        public String[] getSupportedProtocols() {
            return delegate.getSupportedProtocols();
        }

        @Override
        public boolean getUseClientMode() {
            return delegate.getUseClientMode();
        }

        @Override
        public boolean getWantClientAuth() {
            return delegate.getWantClientAuth();
        }

        @Override
        public boolean isInboundDone() {
            return delegate.isInboundDone();
        }

        @Override
        public boolean isOutboundDone() {
            return delegate.isInboundDone();
        }

        @Override
        public void setEnableSessionCreation(boolean arg0) {
            delegate.setEnableSessionCreation(arg0);
        }

        @Override
        public void setEnabledCipherSuites(String[] arg0) {
            delegate.setEnabledCipherSuites(arg0);
        }

        @Override
        public void setEnabledProtocols(String[] arg0) {
            delegate.setEnabledProtocols(arg0);
        }

        @Override
        public void setNeedClientAuth(boolean arg0) {
            delegate.setNeedClientAuth(arg0);
        }

        @Override
        public void setUseClientMode(boolean arg0) {
            delegate.setUseClientMode(arg0);
        }

        @Override
        public void setWantClientAuth(boolean arg0) {
            delegate.setWantClientAuth(arg0);
        }

        @Override
        public SSLEngineResult unwrap(ByteBuffer arg0, ByteBuffer[] arg1, int arg2, int arg3)
                throws SSLException {
            return null;
        }

        @Override
        public SSLEngineResult wrap(ByteBuffer[] arg0, int arg1, int arg2, ByteBuffer arg3)
                throws SSLException {
            return null;
        }

    }

    static class SSLSessionWrapper implements SSLSession {
        SSLSession session;
        Certificate[] certificates;

        SSLSessionWrapper(SSLSession s, Certificate[] certs) {
            this.certificates = certs;
            this.session = s;
        }

        @Override
        public byte[] getId() {
            return session.getId();
        }

        @Override
        public SSLSessionContext getSessionContext() {
            return session.getSessionContext();
        }

        @Override
        public long getCreationTime() {
            return session.getCreationTime();
        }

        @Override
        public long getLastAccessedTime() {
            return session.getLastAccessedTime();
        }

        @Override
        public void invalidate() {
            session.invalidate();
        }

        @Override
        public boolean isValid() {
            return session.isValid();
        }

        @Override
        public void putValue(String s, Object o) {
            session.putValue(s, o);
        }

        @Override
        public Object getValue(String s) {
            return session.getValue(s);
        }

        @Override
        public void removeValue(String s) {
            session.removeValue(s);
        }

        @Override
        public String[] getValueNames() {
            return session.getValueNames();
        }

        @Override
        public Certificate[] getPeerCertificates() throws SSLPeerUnverifiedException {
            return certificates;
        }

        @Override
        public Certificate[] getLocalCertificates() {
            return session.getLocalCertificates();
        }

        @Override
        public Principal getPeerPrincipal() throws SSLPeerUnverifiedException {
            return session.getPeerPrincipal();
        }

        @Override
        public Principal getLocalPrincipal() {
            return session.getLocalPrincipal();
        }

        @Override
        public String getCipherSuite() {
            return session.getCipherSuite();
        }

        @Override
        public String getProtocol() {
            return session.getProtocol();
        }

        @Override
        public String getPeerHost() {
            return session.getPeerHost();
        }

        @Override
        public int getPeerPort() {
            return session.getPeerPort();
        }

        @Override
        public int getPacketBufferSize() {
            return session.getPacketBufferSize();
        }

        @Override
        public int getApplicationBufferSize() {
            return session.getApplicationBufferSize();
        }

        @SuppressWarnings("removal")
        @Override
        public javax.security.cert.X509Certificate[] getPeerCertificateChain() throws SSLPeerUnverifiedException {
            return session.getPeerCertificateChain();
        }
    }

    static class CxfKeyManagerFactory extends KeyManagerFactory {

        CxfKeyManagerFactory(KeyManager... km) {
            super(new KeyManagerFactorySpi() {
                @Override
                protected void engineInit(KeyStore keyStore, char[] pwd) throws KeyStoreException {
                }

                @Override
                protected void engineInit(ManagerFactoryParameters managerFactoryParameters) {
                }

                @Override
                protected KeyManager[] engineGetKeyManagers() {
                    return km;
                }
            }, PROVIDER, "");
        }
    }
}
