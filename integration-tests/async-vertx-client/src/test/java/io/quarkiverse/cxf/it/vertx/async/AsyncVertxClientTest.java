package io.quarkiverse.cxf.it.vertx.async;

import static org.hamcrest.CoreMatchers.is;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.assertj.core.api.Assertions;
import org.eclipse.microprofile.config.ConfigProvider;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import io.quarkiverse.cxf.HTTPConduitImpl;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
@QuarkusTestResource(AsyncVertxClientTestResource.class)
class AsyncVertxClientTest {

    @Test
    void calculatorWithWsdl() {
        HTTPConduitImpl defaultImpl = io.quarkiverse.cxf.HTTPConduitImpl.findDefaultHTTPConduitImpl();
        if (io.quarkiverse.cxf.HTTPConduitImpl.URLConnectionHTTPConduitFactory == defaultImpl) {
            RestAssured.given()
                    .queryParam("a", 7)
                    .queryParam("b", 4)
                    .get("/RestAsyncWithWsdl/calculatorWithWsdl")
                    .then()
                    .statusCode(200)
                    .body(is("11"));
        } else {
            RestAssured.given()
                    .queryParam("a", 7)
                    .queryParam("b", 4)
                    .get("/RestAsyncWithWsdl/calculatorWithWsdl")
                    .then()
                    .statusCode(500)
                    .body(CoreMatchers.containsString(
                            "You have attempted to perform a blocking operation on an IO thread."));
        }

    }

    @Test
    void calculatorWithWsdlWithBlocking() {
        RestAssured.given()
                .queryParam("a", 7)
                .queryParam("b", 4)
                .get("/RestAsyncWithWsdlWithBlocking/calculatorWithWsdlWithBlocking")
                .then()
                .statusCode(200)
                .body(is("11"));
    }

    @Test
    void helloWithWsdlWithEagerInit() {
        RestAssured.given()
                .queryParam("person", "Max")
                .get("/RestAsyncWithWsdlWithEagerInit/helloWithWsdlWithEagerInit")
                .then()
                .statusCode(200)
                .body(is("Hello Max from HelloWithWsdlWithEagerInit"));
    }

    @Test
    void helloWithoutWsdl() {
        RestAssured.given()
                .queryParam("person", "Joe")
                .get("/RestAsyncWithoutWsdl/helloWithoutWsdl")
                .then()
                .statusCode(200)
                .body(is("Hello Joe from HelloWithoutWsdl"));
    }

    @Test
    void helloWithoutWsdlWithBlocking() {
        RestAssured.given()
                .queryParam("person", "Joe")
                .get("/RestAsyncWithoutWsdlWithBlocking/helloWithoutWsdlWithBlocking")
                .then()
                .statusCode(200)
                .body(is("Hello Joe from HelloWithoutWsdlWithBlocking"));
    }

    /**
     * Make sure that our static copy is the same as the WSDL served by the container
     *
     * @throws IOException
     */
    @Test
    void wsdlUpToDate() throws IOException {
        final String wsdlUrl = ConfigProvider.getConfig()
                .getValue("quarkus.cxf.client.calculatorWithWsdl.wsdl", String.class);

        Path staticCopyPath = Paths.get("src/main/resources/wsdl/CalculatorService.wsdl");
        if (!Files.isRegularFile(staticCopyPath)) {
            /*
             * This test can be run from the test jar on Quarkus Platform
             * In that case target/classes does not exist an we have to copy
             * what's needed manually
             */
            staticCopyPath = Paths.get("target/classes/wsdl/CalculatorService.wsdl");
            Files.createDirectories(staticCopyPath.getParent());
            try (InputStream in = AsyncVertxClientTest.class.getClassLoader()
                    .getResourceAsStream("wsdl/CalculatorService.wsdl")) {
                Files.copy(in, staticCopyPath, StandardCopyOption.REPLACE_EXISTING);
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
