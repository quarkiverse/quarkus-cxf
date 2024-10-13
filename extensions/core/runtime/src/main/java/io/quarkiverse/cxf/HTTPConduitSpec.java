package io.quarkiverse.cxf;

import java.io.IOException;
import java.util.Optional;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import org.apache.cxf.Bus;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

import io.quarkiverse.cxf.CxfClientConfig.WellKnownHostnameVerifier;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.runtime.VertxCertificateHolder;
import io.quarkus.tls.runtime.config.TlsBucketConfig;
import io.vertx.core.Vertx;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.KeyStoreOptionsBase;

public interface HTTPConduitSpec {
    default HTTPConduitSpec resolveDefault() {
        return this;
    }

    HTTPConduit createConduit(HTTPTransportFactory f, Bus b, EndpointInfo localInfo, EndpointReferenceType target)
            throws IOException;

    default Optional<TLSClientParameters> tlsClientParameters(CXFClientInfo cxfClientInfo, Vertx vertx) {
        final String hostnameVerifierName = cxfClientInfo.getHostnameVerifier();
        final TlsConfiguration tlsConfig = cxfClientInfo.getTlsConfiguration();
        if (hostnameVerifierName != null || tlsConfig != null) {
            TLSClientParameters tlsCP = createTLSClientParameters(cxfClientInfo);
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
                    if (keyStoreOptions instanceof KeyStoreOptionsBase) {
                        final KeyStoreOptionsBase keyStoreOptionsBase = (KeyStoreOptionsBase) keyStoreOptions;
                        if (keyStoreOptionsBase.getAlias() != null) {
                            tlsCP.setCertAlias(keyStoreOptionsBase.getAlias());
                        }
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

            }
            return Optional.of(tlsCP);
        }
        return Optional.empty();
    }

    default TLSClientParameters createTLSClientParameters(CXFClientInfo cxfClientInfo) {
        final TlsConfiguration tlsConfig = cxfClientInfo.getTlsConfiguration();
        final TLSClientParameters tlsCP = new TLSClientParameters();
        if (tlsConfig instanceof VertxCertificateHolder) {
            final VertxCertificateHolder vertxCertificateHOlder = (VertxCertificateHolder) tlsConfig;
            final TlsBucketConfig bucketConfig = vertxCertificateHOlder.config();
            bucketConfig.cipherSuites().ifPresent(tlsCP::setCipherSuites);

            /*
             * We are not able to transfer some TLS config options from VertxCertificateHolder
             * to the legacy conduit implementations
             */
            if (tlsConfig.isTrustAll()) {
                throw new IllegalStateException(
                        this.getClass().getName().replace("Factory", "")
                                + " does not support quarkus.tls." + cxfClientInfo.getConfigKey() + ".trust-all. ");
            }
            if (tlsConfig.getHostnameVerificationAlgorithm().isPresent()) {
                throw new IllegalStateException(getConduitDescription()
                        + " does not support quarkus.tls." + cxfClientInfo.getConfigKey()
                        + ".hostname-verification-algorithm. Use quarkus.cxf.client." + cxfClientInfo.getConfigKey()
                        + ".hostname-verifier instead.");
            }
            if (bucketConfig.reloadPeriod().isPresent()) {
                throw new IllegalStateException(getConduitDescription()
                        + " does not support quarkus.tls." + cxfClientInfo.getConfigKey()
                        + ".reload-period. Remove the setting and restart the application with the new trust or key stores.");
            }
        }
        return tlsCP;
    }

    String getConduitDescription();

}