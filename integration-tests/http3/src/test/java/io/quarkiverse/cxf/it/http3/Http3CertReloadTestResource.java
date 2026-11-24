package io.quarkiverse.cxf.it.http3;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * Prepares a trust store file that {@link Http3CertReloadTest} can replace while the application is running and
 * configures a SOAP client using it.
 */
public class Http3CertReloadTestResource implements QuarkusTestResourceLifecycleManager {

    /** The trust store containing the certificate of the service */
    static final Path TRUSTING = Path.of("target/classes/localhost-truststore.p12");
    /** A trust store that does not contain the certificate of the service */
    static final Path NON_TRUSTING = Path.of("target/classes/localhost-server-truststore.p12");
    /** The trust store used by the client */
    static final Path RELOADABLE = Path.of("target/classes/reloadable-truststore.p12");
    static final String TLS_CONFIGURATION_NAME = "client-reloadable";

    @Override
    public Map<String, String> start() {
        try {
            Files.copy(NON_TRUSTING, RELOADABLE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return Map.of(
                "quarkus.tls." + TLS_CONFIGURATION_NAME + ".trust-store.p12.path", RELOADABLE.toString(),
                "quarkus.tls." + TLS_CONFIGURATION_NAME + ".trust-store.p12.password", "secret",
                "quarkus.cxf.client.h3CertReload.client-endpoint-url",
                "https://localhost:" + Http3Test.HTTPS_PORT + "/peer/echo",
                "quarkus.cxf.client.h3CertReload.service-interface", ProtocolEcho.class.getName(),
                "quarkus.cxf.client.h3CertReload.tls-configuration-name", TLS_CONFIGURATION_NAME,
                "quarkus.cxf.client.h3CertReload.vertx.http-versions", "3");
    }

    @Override
    public void stop() {
        try {
            Files.deleteIfExists(RELOADABLE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
