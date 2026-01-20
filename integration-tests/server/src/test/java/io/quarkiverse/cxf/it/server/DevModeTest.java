package io.quarkiverse.cxf.it.server;

import static org.hamcrest.CoreMatchers.containsString;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;
import org.l2x6.cli.assured.CommandProcess;
import org.l2x6.mvn.assured.Mvn;

import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;

public class DevModeTest {

    private static final Logger log = Logger.getLogger(DevModeTest.class);

    @Test
    public void devMode() throws IOException, InterruptedException {

        final String quarkusGroupId = getProperty("quarkus.platform.group-id");
        final Object[] versions = {
                quarkusGroupId,
                getProperty("quarkus.platform.artifact-id"),
                getProperty("quarkus.platform.version"),
                getProperty("quarkus-cxf.platform.group-id"),
                getProperty("quarkus-cxf.platform.artifact-id"),
                getProperty("quarkus-cxf.platform.version")
        };

        final String quarkusVersion = getQuarkusVersion();
        final String artifactId = "quarkus-cxf-integration-test-dev-mode";
        final Path tempProject = Path.of("target/" + DevModeTest.class.getSimpleName() + "-" + UUID.randomUUID())
                .resolve(artifactId)
                .toAbsolutePath().normalize();
        try {
            Files.createDirectories(tempProject.getParent());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create " + tempProject.getParent(), e);
        }

        final Mvn mvn = Mvn.fromMvnw(Path.of(".").toAbsolutePath().normalize()).installIfNeeded();
        mvn
                .args(
                        quarkusGroupId + ":quarkus-maven-plugin:" + quarkusVersion + ":create",
                        "-ntp",
                        "-DprojectGroupId=io.quarkiverse.cxf",
                        "-DprojectArtifactId=" + artifactId,
                        "-Dextensions=io.quarkiverse.cxf:quarkus-cxf")
                .cd(tempProject.getParent())
                .then()
                .stdout().log()
                .stderr().log()
                .execute()
                .assertSuccess();

        /* Edit pom.xml */
        final Path pomXmlFile = tempProject.resolve("pom.xml");
        String pomSource = Files.readString(pomXmlFile, StandardCharsets.UTF_8);
        Matcher m = Pattern.compile("<dependencyManagement>.*</dependencyManagement>", Pattern.DOTALL).matcher(pomSource);
        Assertions.assertThat(m.find()).isTrue();
        pomSource = m.replaceFirst("""
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>%s</groupId>
                            <artifactId>%s</artifactId>
                            <version>%s</version>
                            <type>pom</type>
                            <scope>import</scope>
                        </dependency>
                        <dependency>
                            <groupId>%s</groupId>
                            <artifactId>%s</artifactId>
                            <version>%s</version>
                            <type>pom</type>
                            <scope>import</scope>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
                """.formatted(versions));
        Files.writeString(pomXmlFile, pomSource, StandardCharsets.UTF_8);

        /* Copy source files */
        final Path tempSrcMainJava = tempProject.resolve("src/main/java");
        Stream.of(
                Fruit.class,
                FruitService.class,
                FruitServiceImpl.class)
                .forEach(cl -> {
                    final String relJavaFile = relJavaFile(cl);
                    Path dest = tempSrcMainJava.resolve(relJavaFile);
                    try {
                        Files.createDirectories(dest.getParent());
                    } catch (IOException e) {
                        throw new UncheckedIOException("Could not create " + dest.getParent(), e);
                    }
                    final String resource = "DevModeTest/" + cl.getSimpleName() + ".java";
                    try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
                        try {
                            Files.copy(in, dest);
                        } catch (IOException e) {
                            throw new UncheckedIOException("Could not copy " + resource + " to " + dest, e);
                        }
                    } catch (IOException e) {
                        throw new UncheckedIOException("Could not open resource " + resource, e);
                    }
                });
        final Path appProps = tempProject.resolve("src/main/resources/application.properties");
        Files.createDirectories(appProps.getParent());
        Files.writeString(
                appProps,
                """
                        quarkus.cxf.endpoint."/fruits".implementor = io.quarkiverse.cxf.it.server.FruitServiceImpl
                        quarkus.cxf.endpoint."/fruits".logging.enabled = pretty
                        """,
                StandardCharsets.UTF_8);

        /* Run in dev mode */
        CountDownLatch started = new CountDownLatch(1);

        try (CommandProcess mvnProcess = mvn
                .args(
                        "quarkus:dev",
                        "-ntp")
                .cd(tempProject)
                .then()
                .stdout()
                .log(line -> {
                    log.info(line);
                    if (line.contains("Installed features: [")) {
                        started.countDown();
                    }
                })
                .stderr().log()
                .start()) {

            started.await(20, TimeUnit.SECONDS);

            awaitResponse("foo", "bar", "bar");

            /* Change something in Fruit.java */
            final Path fruitJava = tempSrcMainJava.resolve(relJavaFile(Fruit.class));
            Files.writeString(
                    fruitJava,
                    Files.readString(fruitJava, StandardCharsets.UTF_8)
                            .replace("return description;", "return \"Modified: \" + description;"),
                    StandardCharsets.UTF_8);
            awaitResponse("bam", "baz", "Modified: baz");

        }

    }

    static void awaitResponse(String name, String description, String expectedDesciption) {
        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .untilAsserted(
                        () -> {
                            ValidatableResponse response = null;
                            try {
                                log.info("Trying to get response from /services/fruits");
                                response = RestAssured.given()
                                        .header("Content-Type", "text/xml")
                                        .body("""
                                                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ser="http://server.it.cxf.quarkiverse.io/">
                                                    <soapenv:Header/>
                                                    <soapenv:Body>
                                                        <ser:add>
                                                            <arg0>
                                                                <name>%s</name>
                                                                <description>%s</description>
                                                            </arg0>
                                                        </ser:add>
                                                    </soapenv:Body>
                                                </soapenv:Envelope>
                                                """
                                                .formatted(name, description))
                                        .post("http://localhost:8080/services/fruits")
                                        .then();
                            } catch (Exception ex) {
                                // AssertionError keeps Awaitility running
                                log.info("Request didn't work", ex);
                                throw new AssertionError("Error while getting response", ex);
                            }
                            response.statusCode(200)
                                    .body(containsString("<description>" + expectedDesciption + "</description>"));
                        });
    }

    static String getProperty(String key) {
        final String result = System.getProperty(key);
        Assertions.assertThat(result).withFailMessage("System property " + key + " must not be null").isNotBlank();
        return result;
    }

    static String relJavaFile(Class<? extends Object> cl) {
        return cl.getName().replace('.', '/') + ".java";
    }

    static String getQuarkusVersion() {
        Properties props = new Properties();
        try (InputStream is = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream("META-INF/maven/io.quarkus/quarkus-core/pom.properties")) {
            props.load(is);
            return props.getProperty("version");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
