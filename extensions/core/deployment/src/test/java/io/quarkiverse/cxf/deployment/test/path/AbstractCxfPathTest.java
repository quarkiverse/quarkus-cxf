package io.quarkiverse.cxf.deployment.test.path;

import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hamcrest.CoreMatchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkiverse.cxf.deployment.test.Add;
import io.quarkiverse.cxf.deployment.test.Delete;
import io.quarkiverse.cxf.deployment.test.Fruit;
import io.quarkiverse.cxf.deployment.test.FruitWebService;
import io.quarkiverse.cxf.deployment.test.FruitWebServiceImpl;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

/**
 * A base class for testing various combinations of {@code quarkus.http.root-path} and {@code quarkus.cxf.path}.
 */
abstract class AbstractCxfPathTest {

    static QuarkusUnitTest createDeployment(String rootPath, String cxfPath) {
        StringBuilder fruitPath = new StringBuilder();
        if (rootPath != null) {
            fruitPath.append(rootPath);
        }
        if (cxfPath != null) {
            fruitPath.append(cxfPath);
        }
        fruitPath.append("/fruit");

        QuarkusUnitTest result = new QuarkusUnitTest()
                .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                        .addClass(FruitWebService.class)
                        .addClass(FruitWebServiceImpl.class)
                        .addClass(Fruit.class)
                        .addClass(Add.class)
                        .addClass(Delete.class))
                .overrideConfigKey("quarkus.cxf.endpoint.\"/fruit\".implementor",
                        "io.quarkiverse.cxf.deployment.test.FruitWebServiceImpl")
                .overrideConfigKey("quarkus.cxf.client.\"fruitClient\".client-endpoint-url",
                        "http://localhost:8081" + fruitPath.toString());
        if (rootPath != null) {
            result.overrideConfigKey("quarkus.http.root-path", rootPath);
        }
        if (cxfPath != null) {
            result.overrideConfigKey("quarkus.cxf.path", cxfPath);
        } else {
            result.overrideConfigKey("quarkus.cxf.path", "/");
        }
        return result;
    }

    @Inject
    @CXFClient("fruitClient")
    FruitWebService client;

    @ConfigProperty(name = "quarkus.cxf.client.\"fruitClient\".client-endpoint-url")
    String endpointUrl;

    @Test
    public void clientAddGet() {
        client.add(new Fruit("Pear", "Sweet"));
        Assertions.assertThat(client.getDescriptionByName("Pear")).isEqualTo("Sweet");
    }

    @Test
    public void wsdl() {
        RestAssured.get(endpointUrl + "?wsdl")
                .then()
                .statusCode(200)
                .body(CoreMatchers.containsString("<wsdl:operation name=\"getDescriptionByName\">"));
    }

}
