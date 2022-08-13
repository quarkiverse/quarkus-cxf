package io.quarkiverse.it.cxf;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(value = MockWSTestResource.class)
class ClientFacadeResourceTest {

    @Test
    public void testMultiply() {
        given().param("a", 13).param("b", 17)
                .when().get("/rest/clientfacade/multiply")
                .then().statusCode(200).body(is("221"));
    }

    @Test
    public void testAdd() {
        given().param("a", 19).param("b", 23)
                .when().get("/rest/clientfacade/add")
                .then().statusCode(200).body(is("42"));
    }

}
