package io.quarkiverse.cxf.it.security.policy;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.TrustManagerFactory;

import jakarta.enterprise.event.Observes;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HTTPConduitConfigurer;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.runtime.StartupEvent;

public class Init {

    @ConfigProperty(name = "cxf.client-truststore")
    String trustStorePath;
    @ConfigProperty(name = "cxf.client-truststore-type")
    String truststoreType;
    @ConfigProperty(name = "cxf.client-truststore-password")
    String truststorePassword;

    void onStart(@Observes StartupEvent ev) {

        HTTPConduitConfigurer httpConduitConfigurer = new HTTPConduitConfigurer() {
            public void configure(String name, String address, HTTPConduit httpConduit) {
                final KeyStore trustStore;
                final TrustManagerFactory tmf;
                try (InputStream is = Thread.currentThread().getContextClassLoader()
                        .getResourceAsStream(trustStorePath)) {
                    trustStore = KeyStore.getInstance(truststoreType);
                    final String pwd = truststorePassword;
                    trustStore.load(is, pwd == null ? null : pwd.toCharArray());
                    tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                    tmf.init(trustStore);
                } catch (IOException | NoSuchAlgorithmException | CertificateException | KeyStoreException e) {
                    throw new RuntimeException("Could not load client-truststore.jks from class path", e);
                }
                final TLSClientParameters tlsCP = new TLSClientParameters();
                tlsCP.setTrustManagers(tmf.getTrustManagers());
                httpConduit.setTlsClientParameters(tlsCP);
            }
        };
        final Bus bus = BusFactory.getThreadDefaultBus();
        bus.setExtension(httpConduitConfigurer, HTTPConduitConfigurer.class);
    }

}
