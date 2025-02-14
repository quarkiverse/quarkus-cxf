package io.quarkiverse.cxf.it.client.tls;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.smallrye.certs.CertificateGenerator;
import io.smallrye.certs.CertificateRequest;
import io.smallrye.certs.Format;

public class JavaNetSslClientTestResource implements QuarkusTestResourceLifecycleManager {

    private String originalTrustStore;

    @Override
    public Map<String, String> start() {
        try {

            /*
             * Copy $JAVA_HOME/lib/security/cacerts to target/certs,
             * add the localhost certificate into it
             * and set javax.net.ssl.trustStore system property to the modified trust store's path
             */
            final Path certsDir = Path.of("target/certs");
            Files.createDirectories(certsDir);
            final String localhostPassword = "changeit";
            new CertificateGenerator(certsDir, true).generate(new CertificateRequest()
                    .withName("localhost")
                    .withFormat(Format.PKCS12)
                    .withPassword(localhostPassword)
                    .withDuration(Duration.ofDays(2))
                    .withCN("localhost"));

            final String tsType = System.getProperty("javax.net.ssl.trustStoreType", KeyStore.getDefaultType())
                    .toLowerCase(Locale.US);
            final Path systemCacertsPath = defaultTrustStorePath();
            final Path cacertsWithLocalhost = certsDir
                    .resolve("cacerts-with-localhost." + (tsType.equals("pkcs12") ? "p12" : tsType));
            final String cacertsPassword = System.getProperty("javax.net.ssl.trustStorePassword", "changeit");

            final KeyStore cacerts = KeyStore.getInstance(tsType);
            try (InputStream in = Files.newInputStream(systemCacertsPath)) {
                cacerts.load(in, cacertsPassword.toCharArray());
            }

            final KeyStore localhostTruststore = KeyStore.getInstance("PKCS12");
            try (InputStream in = Files.newInputStream(certsDir.resolve("localhost-truststore.p12"))) {
                localhostTruststore.load(in, localhostPassword.toCharArray());
            }
            cacerts.setCertificateEntry("localhost", localhostTruststore.getCertificate("localhost"));

            try (OutputStream out = Files.newOutputStream(cacertsWithLocalhost)) {
                cacerts.store(out, cacertsPassword.toCharArray());
            }

            this.originalTrustStore = System.getProperty("javax.net.ssl.trustStore");
            System.setProperty("javax.net.ssl.trustStore", cacertsWithLocalhost.toString());

            return Map.of(
                    "javax.net.ssl.trustStore", cacertsWithLocalhost.toString(),
                    "quarkus.tls.localhost-pkcs12.key-store.p12.path", certsDir.resolve("localhost-keystore.p12").toString(),
                    "quarkus.tls.localhost-pkcs12.key-store.p12.password", localhostPassword,
                    "quarkus.http.tls-configuration-name", "localhost-pkcs12");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static Path defaultTrustStorePath() {
        final String rawTsPath = System.getProperty("javax.net.ssl.trustStore");
        if (rawTsPath != null && !rawTsPath.isEmpty()) {
            return Path.of(rawTsPath);
        }
        final String javaHome = System.getProperty("java.home");
        if (javaHome == null || javaHome.isEmpty()) {
            throw new IllegalStateException(
                    "Could not locate the default Java truststore because the 'java.home' property is not set");
        }
        final Path javaHomePath = Path.of(javaHome);
        if (!Files.isDirectory(javaHomePath)) {
            throw new IllegalStateException("Could not locate the default Java truststore because the 'java.home' path '"
                    + javaHome + "' is not a directory");
        }
        final Path jssecacerts = javaHomePath.resolve("lib/security/jssecacerts");
        if (Files.isRegularFile(jssecacerts)) {
            return jssecacerts;
        }
        final Path cacerts = javaHomePath.resolve("lib/security/cacerts");
        if (Files.isRegularFile(cacerts)) {
            return cacerts;
        }
        throw new IllegalStateException(
                "Could not locate the default Java truststore. Tried javax.net.ssl.trustStore system property, " + jssecacerts
                        + " and " + cacerts);
    }

    @Override
    public void stop() {
        if (originalTrustStore == null) {
            System.getProperties().remove("javax.net.ssl.trustStore");
        } else {
            System.setProperty("javax.net.ssl.trustStore", originalTrustStore);
        }
    }
}
