package io.quarkiverse.cxf.deployment.test;

import static io.quarkiverse.cxf.test.QuarkusCxfClientTestUtil.anyNs;
import static io.restassured.RestAssured.given;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.xml.namespace.QName;

import jakarta.xml.ws.Service;

import org.awaitility.Awaitility;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.cxf.test.QuarkusCxfClientTestUtil;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.response.ValidatableResponse;

public class ServerDevModeTest {

    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap
                    .create(JavaArchive.class)
                    .addClasses(
                            FruitWebService.class,
                            FruitWebServiceImpl.class,
                            Fruit.class,
                            Add.class,
                            Delete.class)
                    .addAsResource(applicationProperties(), "application.properties"));

    private static final Logger log = Logger.getLogger(ServerDevModeTest.class);

    @Test
    void changePath() {
        RestAssuredConfig config = RestAssured.config();
        config.getXmlConfig().namespaceAware(false);

        final String oldPath = "/fruit";
        final String changedPath = "/new-path";

        assertCount(config, oldPath, 200, "2");
        assertCount(config, changedPath, 404, ""); // does not exist before the change
        assertWsdl(config, oldPath);
        //Assertions.assertThat(
        getClient(FruitWebService.class, "/soap" + oldPath)
                .add(new Fruit("foo", "bar"));

        //                ).isEqualTo(
        //                        Set.of(
        //                                new Fruit("Apple", "Winter fruit"),
        //                                new Fruit("Banana", "Summer fruit"),
        //                                new Fruit("foo", "bar")));

        /* Now change the path of the service */
        TEST.modifyResourceFile("application.properties",
                oldSource -> oldSource.replace("\"" + oldPath + "\"", "\"" + changedPath + "\""));

        assertCount(config, changedPath, 200, "2"); // should work after the change
        assertCount(config, oldPath, 404, ""); // should not work anymore after the change
        assertWsdl(config, changedPath);

        /* One more change: let the count endpoint always return 42 */
        TEST.modifySourceFile(FruitWebServiceImpl.class,
                oldSource -> oldSource.replace("fruits.size()", "42"));
        TEST.modifySourceFile(Fruit.class,
                oldSource -> oldSource.replace("return description;", "return \"Modified: \" + description;"));
        assertCount(config, changedPath, 200, "42"); // should work after the change
        //        Assertions.assertThat(
        getClient(FruitWebService.class, "/soap" + changedPath)
                .add(new Fruit("foo", "bar"));
        //                        )
        //                .isEqualTo(
        //                        Set.of(
        //                                new Fruit("Apple", "Modified: Winter fruit"),
        //                                new Fruit("Banana", "Modified: Summer fruit"),
        //                                new Fruit("foo", "Modified: bar")));

    }

    public static <T> T getClient(Class<T> serviceInterface, String path) {
        try {
            final String namespace = QuarkusCxfClientTestUtil.getDefaultNameSpace(serviceInterface);
            final URL serviceUrl = new URL("http://localhost:8080" + path + "?wsdl");
            final QName qName = new QName(namespace, serviceInterface.getSimpleName());
            final Service service = Service.create(serviceUrl, qName);
            return service.getPort(serviceInterface);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private void assertWsdl(RestAssuredConfig config, String path) {
        given()
                .config(config)
                .when().get("/soap" + path + "?wsdl")
                .then()
                .statusCode(200)
                .body(
                        Matchers.hasXPath(
                                anyNs("definitions", "service", "port", "address") + "/@*[local-name() = 'location']",
                                CoreMatchers.is(
                                        "http://localhost:8080/soap" + path)));
    }

    private void assertCount(RestAssuredConfig config, String path, int expectedStatus, String expectedCount) {
        final String requestBody = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:tem=\"http://test.deployment.cxf.quarkiverse.io/\">\n"
                +
                "   <soapenv:Header/>\n" +
                "   <soapenv:Body>\n" +
                "      <tem:count>\n" +
                "      </tem:count>\n" +
                "   </soapenv:Body>\n" +
                "</soapenv:Envelope>";

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .untilAsserted(
                        () -> {
                            ValidatableResponse response = null;
                            try {
                                log.info("Trying to get response from /soap" + path);
                                response = given()
                                        .config(config)
                                        .body(requestBody)
                                        .when()
                                        .post("/soap" + path)
                                        .then();
                            } catch (Exception ex) {
                                // AssertionError keeps Awaitility running
                                log.info("Request didn't work", ex);
                                throw new AssertionError("Error while getting response", ex);
                            }

                            response.statusCode(expectedStatus);

                            if (expectedStatus >= 200 && expectedStatus < 300) {
                                response.body(
                                        Matchers.hasXPath(
                                                anyNs("Envelope", "Body", "countResponse", "countFruitsResponse") + "/text()",
                                                CoreMatchers.is(expectedCount)));
                            }
                        });
    }

    public static Asset applicationProperties() {
        Writer writer = new StringWriter();
        Properties props = new Properties();
        props.setProperty("quarkus.cxf.path", "/soap");
        props.setProperty("quarkus.cxf.endpoint.\"/fruit\".implementor",
                io.quarkiverse.cxf.deployment.test.FruitWebServiceImpl.class.getName());
        props.setProperty("quarkus.cxf.endpoint.\"/fruit\".logging.enabled", "true");
        try {
            props.store(writer, "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new StringAsset(writer.toString());
    }

}
