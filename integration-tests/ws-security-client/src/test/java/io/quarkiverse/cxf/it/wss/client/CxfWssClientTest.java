package io.quarkiverse.cxf.it.wss.client;

import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.assertj.core.api.Assertions;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
@QuarkusTestResource(CxfWssClientTestResource.class)
public class CxfWssClientTest {

    /**
     * Test whether the traffic logging is present in Quarkus log file
     *
     * @throws IOException
     */
    @Test
    void loggingClient() throws IOException {

        /* Now perform a call that should log something */
        RestAssured.given()
                .queryParam("a", 12)
                .queryParam("b", 8)
                .get("/cxf/wss-client/calculator/modulo")
                .then()
                .statusCode(200)
                .body(is("4"));
    }

    /**
     * Make sure that our static copy is the same as the WSDL served by the container
     *
     * @throws IOException
     */
    @Test
    void wsdlUpToDate() throws IOException {
        final String wsdlUrl = ConfigProvider.getConfig()
                .getValue("quarkus.cxf.client.\"wss-client\".wsdl", String.class);

        final String staticCopyPath = "src/main/wsdl/dir/WssCalculatorService.wsdl";
        /* The changing Docker IP address in the WSDL should not matter */
        final String sanitizerRegex = "<soap:address location=\"http://[^/]*/calculator-ws/WssCalculatorService\"></soap:address>";
        final String staticCopyContent = Files
                .readString(Paths.get(staticCopyPath), StandardCharsets.UTF_8)
                .replaceAll(sanitizerRegex, "");

        final String expected = RestAssured.given()
                .get(wsdlUrl)
                .then()
                .statusCode(200)
                .extract().body().asString();

        if (!expected.replaceAll(sanitizerRegex, "").equals(staticCopyContent)) {
            Files.writeString(Paths.get(staticCopyPath), expected, StandardCharsets.UTF_8);
            Assertions.fail("The static WSDL copy in " + staticCopyPath
                    + " went out of sync with the WSDL served by the container. The content was updated by the test, you just need to review and commit the changes.");
        }

    }

}
