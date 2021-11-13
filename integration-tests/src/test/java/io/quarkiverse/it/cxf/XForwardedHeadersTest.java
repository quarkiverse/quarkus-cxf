package io.quarkiverse.it.cxf;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

@QuarkusTest
@TestProfile(XForwardedProfile.class)
public class XForwardedHeadersTest {

    private static final String X_FORWARDED_PROTO_HEADER = "X-Forwarded-Proto";
    private static final String X_FORWARDED_PREFIX_HEADER = "X-Forwarded-Prefix";
    private static final String X_FORWARDED_HOST_HEADER = "X-Forwarded-Host";
    private static final String X_FORWARDED_PORT_HEADER = "X-Forwarded-Port";

    @Test
    void testXForwardedPrefixHeader() {
        given()
                .when()
                .headers(new Headers(new Header(X_FORWARDED_PREFIX_HEADER, "/test")))
                .get("/soap/greeting?wsdl")
                .then()
                .statusCode(200)
                .body(containsString("http://localhost:8081/test/soap/greeting?wsdl"));
    }

    @Test
    void testXForwardedProtoHeader() {
        given()
                .when()
                .headers(Map.of(
                        X_FORWARDED_PROTO_HEADER, "https"))
                .get("/soap/greeting?wsdl")
                .then()
                .statusCode(200)
                .body(containsString("https://localhost/soap/greeting?wsdl"));
    }

    @Test
    void testXForwardedHostHeader() {
        given()
                .when()
                .headers(Map.of(
                        X_FORWARDED_HOST_HEADER, "api.example.com"))
                .get("/soap/greeting?wsdl")
                .then()
                .statusCode(200)
                .body(containsString("http://api.example.com:8081/soap/greeting?wsdl"));
    }

    @Test
    void testXForwardedPortHeader() {
        given()
                .when()
                .headers(Map.of(
                        X_FORWARDED_PORT_HEADER, "443"))
                .get("/soap/greeting?wsdl")
                .then()
                .statusCode(200)
                .body(containsString("http://localhost:443/soap/greeting?wsdl"));
    }

    @Test
    void testXForwardedHeaders() {
        given()
                .when()
                .headers(Map.of(
                        X_FORWARDED_PREFIX_HEADER, "/test",
                        X_FORWARDED_PROTO_HEADER, "https",
                        X_FORWARDED_HOST_HEADER, "api.example.com",
                        X_FORWARDED_PORT_HEADER, "443"))
                .get("/soap/greeting?wsdl")
                .then()
                .statusCode(200)
                .body(containsString("https://api.example.com:443/test/soap/greeting?wsdl"));
    }

    @Test
    void testXForwardedHeadersParrallel() throws ExecutionException, InterruptedException {
        List<RequestSpecification> specs = List.of(
                given().headers(Map.of(X_FORWARDED_HOST_HEADER, "api1.example.com")),
                given().headers(Map.of(X_FORWARDED_PORT_HEADER, "443")),
                given().headers(Map.of(X_FORWARDED_PREFIX_HEADER, "/test")),
                given().headers(Map.of(X_FORWARDED_PROTO_HEADER, "https",
                        X_FORWARDED_PORT_HEADER, "8280")));

        int requestCount = 20;

        ExecutorService executorService = Executors.newFixedThreadPool(requestCount);
        List<Future<Response>> futures = new ArrayList<>();

        Random random = new Random();
        for (int i = 0; i < requestCount; i++) {
            Future<Response> future = executorService.submit(new SendRequest(specs.get(random.nextInt(specs.size()))));
            futures.add(future);
        }
        executorService.shutdownNow();

        List<Response> responses = new ArrayList<>();
        for (Future<Response> future : futures) {
            responses.add(future.get());
        }

        for (Response response : responses) {
            response.then().body(CoreMatchers.anyOf(
                    containsString("http://api1.example.com:8081/soap/greeting?wsdl"),
                    containsString("http://localhost:443/soap/greeting?wsdl"),
                    containsString("http://localhost:8081/test/soap/greeting?wsdl"),
                    containsString("https://localhost:8280/soap/greeting?wsdl")));
        }
    }

    private static class SendRequest implements Callable<Response> {
        private final RequestSpecification requestSpec;

        public SendRequest(RequestSpecification requestSpec) {
            this.requestSpec = requestSpec;
        }

        @Override
        public Response call() {
            return given(requestSpec).get("/soap/greeting?wsdl");
        }
    }

}
