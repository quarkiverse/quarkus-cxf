package io.quarkiverse.cxf;

import java.security.KeyStore;
import java.util.Optional;

import javax.net.ssl.SSLContext;

import io.quarkus.tls.TlsConfiguration;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.SSLOptions;
import io.vertx.core.net.TrustOptions;

public class CxfTlsConfiguration implements TlsConfiguration {
    private final TrustOptions trustOptions;
    private final KeyStore trustStore;
    private final KeyCertOptions keyStoreOptions;
    private final KeyStore keyStore;

    public CxfTlsConfiguration(KeyCertOptions keyStoreOptions, KeyStore keyStore, TrustOptions trustOptions,
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
    public SSLOptions getSSLOptions() {
        throw new UnsupportedOperationException(getClass().getName() + ".getSSLOptions() is not supported");
    }

    @Override
    public boolean isTrustAll() {
        throw new UnsupportedOperationException(getClass().getName() + ".isTrustAll() is not supported");
    }

    @Override
    public Optional<String> getHostnameVerificationAlgorithm() {
        throw new UnsupportedOperationException(getClass().getName() + ".getHostnameVerificationAlgorithm() is not supported");
    }

    @Override
    public boolean usesSni() {
        throw new UnsupportedOperationException(getClass().getName() + ".usesSni() is not supported");
    }

    @Override
    public boolean reload() {
        throw new UnsupportedOperationException(getClass().getName() + ".reload() is not supported");
    }
}
