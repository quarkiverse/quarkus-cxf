package io.quarkiverse.cxf.it.clustering;

import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.awaitility.Awaitility;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.testcontainers.containers.GenericContainer;

@QuarkusTest
@QuarkusTestResource(CxfClusteringTestResource1.class)
@QuarkusTestResource(CxfClusteringTestResource2.class)
public class FailoverFeatureTest {
    private static final Pattern INSTALLED_FEATURES_PATTERN = Pattern.compile("Installed features: \\[[^\\]]*cxf[^\\]]*\\]");

    private GenericContainer<?> calculatorContainer2;

    /**
     * Test whether the failover occurs in Quarkus log file
     *
     * @throws IOException
     */
    @Test
    void failoverClient() throws IOException {
        final Path logFile = Paths.get(ConfigProvider.getConfig().getValue("quarkus.log.file.path", String.class));

        String calculator1BaseUri = ConfigProvider.getConfig().getValue("cxf.it.calculator1.baseUri", String.class);
        String calculator2BaseUri = ConfigProvider.getConfig().getValue("cxf.it.calculator2.baseUri", String.class);

        /* Make sure the server has started */
        Awaitility.waitAtMost(30, TimeUnit.SECONDS)
                .until(
                        () -> {
                            if (Files.isRegularFile(logFile)) {
                                final String content = Files.readString(logFile, StandardCharsets.UTF_8);
                                if (INSTALLED_FEATURES_PATTERN.matcher(content).find()) {
                                    return true;
                                }
                            }
                            return false;
                        });

        /* Perform a call to the default endpoint (calculatorContainer2) */
        RestAssured.given()
                .queryParam("a", 3)
                .queryParam("b", 4)
                .get("/cxf/clustering/failoverCalculator/multiply")
                .then()
                .statusCode(200)
                .body(is("12"));

        /* Stop the container */
        calculatorContainer2.stop();

        /* Perform a second call - this should fail over to calculatorContainer1) */
        RestAssured.given()
                .queryParam("a", 5)
                .queryParam("b", 6)
                .get("/cxf/clustering/failoverCalculator/multiply")
                .then()
                .statusCode(200)
                .body(is("30"));

        /* ... and check that the stuff was really logged */
        Awaitility.waitAtMost(30, TimeUnit.SECONDS)
                .until(
                        () -> {
                            if (Files.isRegularFile(logFile)) {
                                final String content = Files.readString(logFile, StandardCharsets.UTF_8);
                                if (content.contains("Using failover strategy org.apache.cxf.clustering.SequentialStrategy") &&
                                        content.contains("failing over to alternate address " + calculator1BaseUri) &&
                                        !content.contains("failing over to alternate address " + calculator2BaseUri)) {
                                    return true;
                                }
                            }
                            return false;
                        });
    }

}
