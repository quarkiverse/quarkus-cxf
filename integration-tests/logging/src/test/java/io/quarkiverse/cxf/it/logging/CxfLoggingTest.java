package io.quarkiverse.cxf.it.logging;

import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
@QuarkusTestResource(CxfLoggingTestResource.class)
public class CxfLoggingTest {
    private static final Pattern INSTALLED_FEATURES_PATTERN = Pattern.compile("Installed features: \\[[^\\]]*cxf[^\\]]*\\]");

    /**
     * Test whether the traffic logging is present in Quarkus log file
     *
     * @throws IOException
     */
    @Test
    void loggingClient() throws IOException {
        final Path logFile = Paths.get(ConfigProvider.getConfig().getValue("quarkus.log.file.path", String.class));

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
        Assertions.assertThat(Files.readString(logFile, StandardCharsets.UTF_8)).doesNotContain("org.apa.cxf.ser.Cal.REQ_OUT");

        /* Now perform a call that should log something */
        RestAssured.given()
                .queryParam("a", 3)
                .queryParam("b", 4)
                .get("/cxf/logging/calculator/multiply")
                .then()
                .statusCode(200)
                .body(is("12"));

        /* ... and check that the stuff was really logged */
        Awaitility.waitAtMost(30, TimeUnit.SECONDS)
                .until(
                        () -> {
                            if (Files.isRegularFile(logFile)) {
                                final String content = Files.readString(logFile, StandardCharsets.UTF_8);
                                if (content.contains("org.apa.cxf.ser.Cal.REQ_OUT")
                                        && content.contains(
                                                "Payload: <soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                                                        + "  <soap:Body>\n"
                                                        + "    <ns2:multiply xmlns:ns2=\"http://www.jboss.org/eap/quickstarts/wscalculator/Calculator\">\n"
                                                        + "      <arg0>3</arg0>\n"
                                                        + "      <arg1>4</arg1>\n"
                                                        + "    </ns2:multiply>\n"
                                                        + "  </soap:Body>\n"
                                                        + "</soap:Envelope>")
                                        && content.contains("org.apa.cxf.ser.Cal.RESP_IN")
                                        && content.contains(
                                                "Payload: <soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                                                        + "  <soap:Body>\n"
                                                        + "    <ns2:multiplyResponse xmlns:ns2=\"http://www.jboss.org/eap/quickstarts/wscalculator/Calculator\">\n"
                                                        + "      <return>12</return>\n"
                                                        + "    </ns2:multiplyResponse>\n"
                                                        + "  </soap:Body>\n"
                                                        + "</soap:Envelope>")) {
                                    return true;
                                }
                            }
                            return false;
                        });
    }

    /**
     * Make sure that our static copy is the same as the WSDL served by the container
     *
     * @throws IOException
     */
    @Test
    void wsdlUpToDate() throws IOException {
        final String wsdlUrl = ConfigProvider.getConfig()
                .getValue("quarkus.cxf.client.\"logging-client\".wsdl", String.class);

        Path staticCopyPath = Paths.get("src/main/resources/wsdl/CalculatorService.wsdl");
        if (!Files.isRegularFile(staticCopyPath)) {
            /*
             * This test can be run from the test jar on Quarkus Platform
             * In that case target/classes does not exist an we have to copy
             * what's needed manually
             */
            staticCopyPath = Paths.get("target/classes/wsdl/CalculatorService.wsdl");
            Files.createDirectories(staticCopyPath.getParent());
            try (InputStream in = CxfLoggingTest.class.getClassLoader().getResourceAsStream("wsdl/CalculatorService.wsdl")) {
                Files.copy(in, staticCopyPath);
            }
        }
        /* The changing Docker IP address in the WSDL should not matter */
        final String sanitizerRegex = "<soap:address location=\"http://[^/]*/calculator-ws/CalculatorService\"></soap:address>";
        final String staticCopyContent = Files
                .readString(staticCopyPath, StandardCharsets.UTF_8)
                .replaceAll(sanitizerRegex, "");

        final String expected = RestAssured.given()
                .get(wsdlUrl)
                .then()
                .statusCode(200)
                .extract().body().asString();

        if (!expected.replaceAll(sanitizerRegex, "").equals(staticCopyContent)) {
            Files.writeString(staticCopyPath, expected, StandardCharsets.UTF_8);
            Assertions.fail("The static WSDL copy in " + staticCopyPath
                    + " went out of sync with the WSDL served by the container. The content was updated by the test, you just need to review and commit the changes.");
        }

    }

}
