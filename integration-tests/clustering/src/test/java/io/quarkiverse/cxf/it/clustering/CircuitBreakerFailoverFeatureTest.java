package io.quarkiverse.cxf.it.clustering;

import static org.hamcrest.Matchers.is;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.awaitility.Awaitility;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * {@code CircuitBreakerFailoverFeature} set in {@code application.properties}
 */
@QuarkusTest
@QuarkusTestResource(CxfClusteringTestResource1.class)
@QuarkusTestResource(CxfClusteringTestResource2.class)
@Disabled
public class CircuitBreakerFailoverFeatureTest {

    private static final Pattern INSTALLED_FEATURES_PATTERN = Pattern.compile("Installed features: \\[[^\\]]*cxf[^\\]]*\\]");

    private GenericContainer<?> calculatorContainer2;

    @Test
    public void circuitBreakerFailover() {
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
        /* Make sure we are not reading from a file that was written by a previous run of this test */
        //Assertions.assertThat(Files.readString(logFile, StandardCharsets.UTF_8)).doesNotContain("org.apa.cxf.ser.Cal.REQ_OUT");

        /* Now perform two calls that should log something */
        RestAssured.given()
                .queryParam("a", 3)
                .queryParam("b", 4)
                .get("/cxf/clustering/circuitBreakerCalculator/multiply")
                .then()
                .statusCode(200)
                .body(is("12"));

        RestAssured.given()
                .queryParam("a", 2)
                .queryParam("b", 8)
                .get("/cxf/clustering/circuitBreakerCalculator/multiply")
                .then()
                .statusCode(200)
                .body(is("16"));

        /* ... and check that the stuff was really logged */
        Awaitility.waitAtMost(30, TimeUnit.SECONDS)
                .until(
                        () -> {
                            if (Files.isRegularFile(logFile)) {
                                final String content = Files.readString(logFile, StandardCharsets.UTF_8);
                                if (content.contains("Using failover strategy org.apache.cxf.clustering.SequentialStrategy") &&
                                        content.contains("failing over to alternate address " + calculator1BaseUri) &&
                                        content.contains("failing over to alternate address " + calculator2BaseUri)) {
                                    return true;
                                }
                            }
                            return false;
                        });
    }
}
