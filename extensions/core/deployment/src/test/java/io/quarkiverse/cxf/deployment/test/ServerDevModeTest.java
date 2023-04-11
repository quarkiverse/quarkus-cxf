package io.quarkiverse.cxf.deployment.test;

import static io.quarkiverse.cxf.test.QuarkusCxfClientTestUtil.anyNs;
import static io.restassured.RestAssured.given;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Properties;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

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

    @Test
    void changePath() {
        RestAssuredConfig config = RestAssured.config();
        config.getXmlConfig().namespaceAware(false);

        final String oldPath = "/fruit";
        final String changedPath = "/new-path";

        assertCount(config, oldPath, 200, "2");
        assertCount(config, changedPath, 404, ""); // does not exist before the change
        assertWsdl(config, oldPath);

        /* Now change the path of the service */
        TEST.modifyResourceFile("application.properties",
                oldSource -> oldSource.replace("\"" + oldPath + "\"", "\"" + changedPath + "\""));

        assertCount(config, changedPath, 200, "2"); // should work after the change
        assertCount(config, oldPath, 404, ""); // should not work anymore after the change
        assertWsdl(config, changedPath);

        /* One more change: let the count endpoint always return 42 */
        TEST.modifySourceFile(FruitWebServiceImpl.class,
                oldSource -> oldSource.replace("fruits.size()", "42"));
        assertCount(config, changedPath, 200, "42"); // should work after the change

    }

    private void assertWsdl(RestAssuredConfig config, String path) {
        given()
                .config(config)
                .when().get(path + "?wsdl")
                .then()
                .statusCode(200)
                .body(
                        Matchers.hasXPath(
                                anyNs("definitions", "service", "port", "address") + "/@*[local-name() = 'location']",
                                CoreMatchers.is(
                                        "http://localhost:8080" + path)));
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

        final ValidatableResponse response = given()
                .config(config)
                .body(requestBody)
                .when()
                .post(path)
                .then()
                .statusCode(expectedStatus);

        if (expectedStatus >= 200 && expectedStatus < 300) {
            response.body(
                    Matchers.hasXPath(
                            anyNs("Envelope", "Body", "countResponse", "countFruitsResponse") + "/text()",
                            CoreMatchers.is(expectedCount)));
        }
    }

    public static Asset applicationProperties() {
        Writer writer = new StringWriter();
        Properties props = new Properties();
        props.setProperty("quarkus.cxf.path", "/");
        props.setProperty("quarkus.cxf.endpoint.\"/fruit\".implementor",
                io.quarkiverse.cxf.deployment.test.FruitWebServiceImpl.class.getName());
        try {
            props.store(writer, "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new StringAsset(writer.toString());
    }

}
