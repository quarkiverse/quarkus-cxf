package io.quarkiverse.cxf.it.auth.mtls;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.assertj.core.api.Assertions;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
// tag::smallrye-cert-gen[]
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Alias;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;

@Certificates(baseDir = "target/classes", //
        certificates = @Certificate( //
                name = "localhost", //
                password = "secret", //
                aliases = @Alias(//
                        name = "client", //
                        password = "secret", //
                        client = true), //
                formats = { Format.PKCS12, Format.JKS }))
@QuarkusTest
public class MutualTlsTest {
    // end::smallrye-cert-gen[]

    @ParameterizedTest
    @ValueSource(strings = { "mTls", "mTlsOld" })
    void mTls(String clientName) throws IOException {

        final Config config = ConfigProvider.getConfig();
        final String keystoreType = config.getValue("keystore.type.short", String.class);
        final Path keystorePath = Path.of("target/classes/localhost-client-keystore." + keystoreType);
        extract(keystorePath);

        final Path truststorePath = Path.of("target/classes/localhost-truststore." + keystoreType);
        extract(truststorePath);

        ExtractableResponse<Response> response = RestAssured.given()
                .config(restAssuredConfig())
                .body("Sam")
                .post("https://localhost:8444/cxf/mtls-rest/" + clientName)
                .then()
                .extract();
        if (response.statusCode() != 200) {
            Assertions.fail("Expected 200, got " + response.statusCode() + ": " + response.body().asString());
        } else {
            Assertions.assertThat(response.body().asString()).isEqualTo("Hello Sam authenticated by mTLS!");
        }
    }

    private void extract(final Path keystorePath) throws IOException {
        Assertions.assertThat(keystorePath.getName(0).toString()).isEqualTo("target");
        if (!Files.isRegularFile(keystorePath)) {
            /*
             * This test can be run from the test jar on Quarkus Platform
             * In that case target/classes does not exist an we have to copy
             * what's needed manually
             */
            Files.createDirectories(keystorePath.getParent());
            try (InputStream in = MutualTlsTest.class.getClassLoader()
                    .getResourceAsStream(keystorePath.getFileName().toString())) {
                Files.copy(in, keystorePath, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    @Test
    void noKeystore() {
        RestAssured.given()
                .config(restAssuredConfig())
                .body("Sam")
                .post("https://localhost:8444/cxf/mtls-rest/noKeystore")
                .then()
                .statusCode(500)
                .body(Matchers.anyOf(
                        /* On Linux, we randomly get any of the following: */
                        Matchers.containsString("SSLHandshakeException: Received fatal alert: bad_certificate"),
                        Matchers.containsString("IOException: Error writing to server"),
                        /*
                         * This comes sometimes with the Vert.x client - see
                         * https://github.com/quarkiverse/quarkus-cxf/issues/1429
                         */
                        Matchers.containsString("io.netty.channel.StacklessClosedChannelException"),
                        /* On Windows, we get this */
                        Matchers.containsString(
                                "java.net.SocketException: An established connection was aborted by the software in your host machine")));

    }

    public static RestAssuredConfig restAssuredConfig() {
        final Config config = ConfigProvider.getConfig();
        final String ext = config.getValue("keystore.type.short", String.class);
        return RestAssured.config().sslConfig(new SSLConfig().with()
                .trustStore(
                        "localhost-truststore." + ext,
                        "secret")
                .keyStore(
                        "localhost-client-keystore." + ext,
                        "secret"));
    }

}
