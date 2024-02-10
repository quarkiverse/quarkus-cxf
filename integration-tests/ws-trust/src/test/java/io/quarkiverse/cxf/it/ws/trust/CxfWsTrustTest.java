package io.quarkiverse.cxf.it.ws.trust;

import static io.quarkiverse.cxf.test.QuarkusCxfClientTestUtil.anyNs;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;

@QuarkusTest
public class CxfWsTrustTest {

    /**
     * Make sure the ws-trust-1.4-service.wsdl file is served
     */
    @Test
    void stsWsdl() {
        RestAssuredConfig config = RestAssured.config();
        config.getXmlConfig().namespaceAware(false);
        given()
                .config(config)
                .when().get("/services/sts?wsdl")
                .then()
                .statusCode(200)
                .body(
                        Matchers.hasXPath(
                                anyNs("definitions", "Policy")
                                        + "/@*[local-name() = 'Id']",
                                CoreMatchers.is("UT_policy")));
    }

    @Test
    void wsdl() {
        RestAssuredConfig config = RestAssured.config();
        config.getXmlConfig().namespaceAware(false);
        given()
                .config(config)
                .when().get("/services/hello-ws-trust?wsdl")
                .then()
                .statusCode(200)
                .body(
                        Matchers.hasXPath(
                                anyNs("definitions", "Policy")
                                        + "[1]/@*[local-name() = 'Id']",
                                CoreMatchers.is("AsymmetricSAML2Policy")),
                        Matchers.hasXPath(
                                anyNs("definitions", "Policy")
                                        + "[2]/@*[local-name() = 'Id']",
                                CoreMatchers.is("io-policy")));
    }

    @Test
    public void stsClient() throws Exception {

        RestAssured.given()
                .body("Frank")
                .post("/ws-trust/hello/hello-ws-trust")
                .then()
                .statusCode(200)
                .body(is("Hello Frank!"));

    }

    @Test
    public void stsClientBean() throws Exception {

        RestAssured.given()
                .body("Frank")
                .post("/ws-trust/hello/hello-ws-trust-bean")
                .then()
                .statusCode(200)
                .body(is("Hello Frank!"));

    }

}
