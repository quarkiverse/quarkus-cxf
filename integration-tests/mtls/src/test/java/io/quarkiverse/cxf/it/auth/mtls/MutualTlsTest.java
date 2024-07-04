package io.quarkiverse.cxf.it.auth.mtls;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.assertj.core.api.Assertions;
import org.eclipse.microprofile.config.ConfigProvider;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

@QuarkusTest
public class MutualTlsTest {
    @Test
    void mTls() throws IOException {

        final Path keystorePath = Path
                .of(ConfigProvider.getConfig().getValue("quarkus.cxf.client.mTls.key-store", String.class));
        extract(keystorePath);

        final Path truststorePath = Path
                .of(ConfigProvider.getConfig().getValue("quarkus.cxf.client.mTls.trust-store", String.class));
        extract(truststorePath);

        ExtractableResponse<Response> response = RestAssured.given()
                .config(restAssuredConfig())
                .body("Sam")
                .post("https://localhost:8444/cxf/mtls-rest/mTls")
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
        return RestAssured.config().sslConfig(new SSLConfig().with()
                .trustStore(
                        "client-truststore." + ConfigProvider.getConfig().getValue("keystore.type", String.class),
                        "client-truststore-password")
                .keyStore(
                        "client-keystore." + ConfigProvider.getConfig().getValue("keystore.type", String.class),
                        "client-keystore-password"));
    }

}
