package io.quarkiverse.cxf;

import java.security.KeyStore;
import java.util.Optional;

import javax.net.ssl.SSLContext;

import io.quarkus.tls.TlsConfiguration;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.SSLOptions;
import io.vertx.core.net.TrustOptions;

/**
 * A {@link TlsConfiguration} implementation used in case the user uses the legacy way of implementing TLS
 * via {@code .trust-store*} and {@code .key-store*} family of options rather than via {@code .tls-configuration-name}.
 */
public class CxfTlsConfiguration implements TlsConfiguration {
    private final TrustOptions trustOptions;
    private final KeyStore trustStore;
    private final KeyCertOptions keyStoreOptions;
    private final KeyStore keyStore;

    public CxfTlsConfiguration(
            KeyCertOptions keyStoreOptions,
            KeyStore keyStore,
            TrustOptions trustOptions,
            KeyStore trustStore) {
        this.keyStoreOptions = keyStoreOptions;
        this.keyStore = keyStore;
        this.trustOptions = trustOptions;
        this.trustStore = trustStore;
    }

    @Override
    public KeyCertOptions getKeyStoreOptions() {
        return keyStoreOptions;
    }

    @Override
    public KeyStore getKeyStore() {
        return keyStore;
    }

    @Override
    public TrustOptions getTrustStoreOptions() {
        return trustOptions;
    }

    @Override
    public KeyStore getTrustStore() {
        return trustStore;
    }

    @Override
    public SSLContext createSSLContext() throws Exception {
        throw new UnsupportedOperationException(getClass().getName() + ".createSSLContext() is not supported");
    }

    @Override
    public synchronized SSLOptions getSSLOptions() {
        SSLOptions options = new SSLOptions();
        options.setKeyCertOptions(getKeyStoreOptions());
        options.setTrustOptions(getTrustStoreOptions());
        //        options.setUseAlpn(config().alpn());
        //        options.setSslHandshakeTimeoutUnit(TimeUnit.SECONDS);
        //        options.setSslHandshakeTimeout(config().handshakeTimeout().toSeconds());
        //        options.setEnabledSecureTransportProtocols(config().protocols());
        //
        //        for (Buffer buffer : crls) {
        //            options.addCrlValue(buffer);
        //        }
        //
        //        for (String cipher : config().cipherSuites().orElse(Collections.emptyList())) {
        //            options.addEnabledCipherSuite(cipher);
        //        }

        return options;
    }

    @Override
    public boolean isTrustAll() {
        return false;
    }

    @Override
    public Optional<String> getHostnameVerificationAlgorithm() {
        /*
         * If the user does not use .tls-configuration-name (which is the case when CxfTlsConfiguration is used)
         * then there is no way for him to set the hostnameVerificationAlgorithm.
         */
        return Optional.empty();
    }

    @Override
    public boolean usesSni() {
        /* There is no way to configure SNI via TLSClientParameters */
        return false;
    }

    @Override
    public boolean reload() {
        throw new UnsupportedOperationException(getClass().getName() + ".reload() is not supported");
    }
}
