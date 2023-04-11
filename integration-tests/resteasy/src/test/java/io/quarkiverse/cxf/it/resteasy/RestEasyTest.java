package io.quarkiverse.cxf.it.resteasy;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkiverse.cxf.test.QuarkusCxfClientTestUtil;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class RestEasyTest {

    @Test
    public void helloCxf() {
        final HelloService client = QuarkusCxfClientTestUtil.getClient(HelloService.class, "/hello");
        org.assertj.core.api.Assertions.assertThat(client.hello("World")).isEqualTo("Hello World from CXF!");
    }

    @Test
    public void helloRestEasy() {
        RestAssured.given()
                .get("/rest/hello/Joe")
                .then()
                .statusCode(200)
                .body(is("Hello from RESTEasy Joe!"));
    }

}
