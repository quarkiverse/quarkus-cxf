package io.quarkiverse.cxf.it.logging;

import static org.hamcrest.Matchers.is;

import java.io.IOException;
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

import io.quarkiverse.cxf.client.it.CxfClientTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
@QuarkusTestResource(CxfClientTestResource.class)
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
                .get("/cxf/logging/beanConfiguredCalculator/multiply")
                .then()
                .statusCode(200)
                .body(is("12"));

        RestAssured.given()
                .queryParam("a", 3)
                .queryParam("b", 4)
                .get("/cxf/logging/propertiesConfiguredCalculator/add")
                .then()
                .statusCode(200)
                .body(is("7"));

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
                                                        + "</soap:Envelope>")
                                        && content.contains(
                                                "Payload: <soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                                                        + "  <soap:Body>\n"
                                                        + "    <ns2:add xmlns:ns2=\"http://www.jboss.org/eap/quickstarts/wscalculator/Calculator\">\n"
                                                        + "      <arg0>3</arg0>\n"
                                                        + "      <arg1>4</arg1>\n"
                                                        + "    </ns2:add>\n"
                                                        + "  </soap:Body>\n"
                                                        + "</soap:Envelope>")
                                        && content.contains(
                                                "Payload: <soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                                                        + "  <soap:Body>\n"
                                                        + "    <ns2:addResponse xmlns:ns2=\"http://www.jboss.org/eap/quickstarts/wscalculator/Calculator\">\n"
                                                        + "      <return>7</return>\n"
                                                        + "    </ns2:addResponse>\n"
                                                        + "  </soap:Body>\n"
                                                        + "</soap:Envelope>")) {
                                    return true;
                                }
                            }
                            return false;
                        });
    }

}
