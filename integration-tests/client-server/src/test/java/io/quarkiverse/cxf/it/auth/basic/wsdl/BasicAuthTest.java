package io.quarkiverse.cxf.it.auth.basic.wsdl;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class BasicAuthTest {

    @Test
    public void getSecureWsdlAnonymous() {
        RestAssured.given()
                .get("/soap/basicAuthSecureWsdl?wsdl")
                .then()
                .statusCode(401);
    }

    @Test
    public void getSecureWsdlBasicGoodUserPreemptive() {
        RestAssured
                .given()
                .auth().preemptive().basic("bob", "bob234")
                .get("/soap/basicAuthSecureWsdl?wsdl")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("http://schemas.xmlsoap.org/wsdl/"));
    }

    @Test
    public void getSecureWsdlBasicBadUser() {
        /* The user exists, can authenticate, but does not have the right role */
        RestAssured
                .given()
                .auth().preemptive().basic("alice", "alice123")
                .get("/soap/basicAuthSecureWsdl?wsdl")
                .then()
                .statusCode(403);
    }

    @Test
    public void basicAuthSecureWsdlGoodUser() {
        /* WSDL secured by basic auth */
        RestAssured
                .given()
                .body("Mary")
                .post("/client-server/basic-auth/basicAuthSecureWsdl/hello")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("Hello Mary!"));

    }

    @Test
    public void basicAuthGoodUser() {
        /* WSDL not secured by basic auth */
        RestAssured
                .given()
                .body("Mary")
                .post("/client-server/basic-auth/basicAuth/hello")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("Hello Mary!"));

    }

}
