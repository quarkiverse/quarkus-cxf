package io.quarkiverse.cxf.it.http3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.tls.CertificateUpdatedEvent;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Alias;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;

/**
 * A SOAP client having {@code 3} in {@code quarkus.cxf.client."client-name".vertx.http-versions} picks up the changes
 * of its TLS configuration when the TLS configuration is reloaded.
 * <p>
 * There is no native variant of this test, because it reloads the TLS configuration through the beans of the
 * application.
 */
@Certificates(baseDir = "target/classes", //
        certificates = @Certificate( //
                name = "localhost", //
                password = "secret", //
                aliases = @Alias(//
                        name = "client", //
                        password = "secret", //
                        client = true), //
                formats = { Format.PKCS12 }))
@QuarkusTestResource(value = Http3CertReloadTestResource.class, restrictToAnnotatedClass = true)
@QuarkusTest
public class Http3CertReloadTest {

    @CXFClient("h3CertReload")
    ProtocolEcho client;

    @Inject
    TlsConfigurationRegistry registry;

    @Inject
    Event<CertificateUpdatedEvent> event;

    @Test
    void trustStoreReload() throws IOException {
        /* The initial trust store of the client does not contain the certificate of the service */
        assertUntrusted();

        /* Once the client gets the right trust store, it is able to open HTTP/3 connections */
        replaceTrustStore(Http3CertReloadTestResource.TRUSTING);
        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(200))
                /* The client keeps failing until it has taken over the new trust store */
                .ignoreExceptions()
                .untilAsserted(() -> assertThat(client.echo("x")).isEqualTo("HTTP/3.0:1"));

        /* ... and once the right trust store is gone, the client does not use the connection opened with it anymore */
        replaceTrustStore(Http3CertReloadTestResource.NON_TRUSTING);
        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(this::assertUntrusted);
    }

    private void assertUntrusted() {
        assertThatThrownBy(() -> client.echo("x"))
                .satisfiesAnyOf(
                        e -> assertThat(e).hasStackTraceContaining("CERTIFICATE_VERIFY_FAILED"),
                        e -> assertThat(e).hasStackTraceContaining("QUICHE_ERR_TLS_FAIL"));
    }

    /**
     * Replaces the trust store of the client with the given {@code newTrustStore} and makes the application reload it.
     *
     * @param newTrustStore the trust store to use from now on
     */
    private void replaceTrustStore(Path newTrustStore) throws IOException {
        Files.copy(newTrustStore, Http3CertReloadTestResource.RELOADABLE, StandardCopyOption.REPLACE_EXISTING);
        final String name = Http3CertReloadTestResource.TLS_CONFIGURATION_NAME;
        final TlsConfiguration tlsConfiguration = registry.get(name).orElseThrow();
        assertThat(tlsConfiguration.reload()).isTrue();
        event.fire(new CertificateUpdatedEvent(name, tlsConfiguration));
    }
}
