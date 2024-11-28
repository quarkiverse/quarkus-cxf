package io.quarkiverse.cxf.it.server;

import static io.quarkiverse.cxf.test.QuarkusCxfClientTestUtil.anyNs;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
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
                .header(X_FORWARDED_PREFIX_HEADER, "/test")
                .get("/soap/greeting?wsdl")
                .then()
                .statusCode(200)
                .body(
                        Matchers.hasXPath(
                                anyNs("definitions", "service", "port", "address") + "/@*[local-name() = 'location']",
                                CoreMatchers.is(
                                        "http://localhost:8081/test/soap/greeting")));
    }

    @Test
    void testXForwardedProtoHeader() {
        given()
                .when()
                .header(X_FORWARDED_PROTO_HEADER, "https")
                .get("/soap/greeting?wsdl")
                .then()
                .statusCode(200)
                .body(
                        Matchers.hasXPath(
                                anyNs("definitions", "service", "port", "address") + "/@*[local-name() = 'location']",
                                CoreMatchers.is(
                                        "https://localhost/soap/greeting")));
    }

    @Test
    void testXForwardedHostHeader() {
        given()
                .when()
                .header(X_FORWARDED_HOST_HEADER, "api.example.com")
                .get("/soap/greeting?wsdl")
                .then()
                .statusCode(200)
                .body(
                        Matchers.hasXPath(
                                anyNs("definitions", "service", "port", "address") + "/@*[local-name() = 'location']",
                                CoreMatchers.is(
                                        "http://api.example.com/soap/greeting")));
    }

    @Test
    void testXForwardedPortHeader() {
        given()
                .when()
                .header(X_FORWARDED_PORT_HEADER, "1234")
                .get("/soap/greeting?wsdl")
                .then()
                .statusCode(200)
                .body(
                        Matchers.hasXPath(
                                anyNs("definitions", "service", "port", "address") + "/@*[local-name() = 'location']",
                                CoreMatchers.is(
                                        "http://localhost:1234/soap/greeting")));
        given()
                .when()
                .header(X_FORWARDED_PORT_HEADER, "80")
                .get("/soap/greeting?wsdl")
                .then()
                .statusCode(200)
                .body(
                        Matchers.hasXPath(
                                anyNs("definitions", "service", "port", "address") + "/@*[local-name() = 'location']",
                                CoreMatchers.is(
                                        "http://localhost/soap/greeting")));
        given()
                .when()
                .header(X_FORWARDED_PROTO_HEADER, "https")
                .header(X_FORWARDED_PORT_HEADER, "443")
                .get("/soap/greeting?wsdl")
                .then()
                .statusCode(200)
                .body(
                        Matchers.hasXPath(
                                anyNs("definitions", "service", "port", "address") + "/@*[local-name() = 'location']",
                                CoreMatchers.is(
                                        "https://localhost/soap/greeting")));
    }

    @Test
    void testXForwardedHeaders() {
        given()
                .when()
                .header(X_FORWARDED_PREFIX_HEADER, "/test")
                .header(X_FORWARDED_PROTO_HEADER, "https")
                .header(X_FORWARDED_HOST_HEADER, "api.example.com")
                .header(X_FORWARDED_PORT_HEADER, "1234")
                .get("/soap/greeting?wsdl")
                .then()
                .statusCode(200)
                .body(
                        Matchers.hasXPath(
                                anyNs("definitions", "service", "port", "address") + "/@*[local-name() = 'location']",
                                CoreMatchers.is(
                                        "https://api.example.com:1234/test/soap/greeting")));
    }

    @Test
    void testXForwardedHeadersParrallel() throws ExecutionException, InterruptedException {
        List<RequestSpecification> specs = List.of(
                given().header(X_FORWARDED_HOST_HEADER, "api1.example.com"),
                given().header(X_FORWARDED_PORT_HEADER, "443"),
                given().header(X_FORWARDED_PREFIX_HEADER, "/test"),
                given().header(X_FORWARDED_PROTO_HEADER, "https").header(X_FORWARDED_PORT_HEADER, "8280"));

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
                    containsString("http://api1.example.com/soap/greeting"),
                    containsString("http://localhost:443/soap/greeting"),
                    containsString("http://localhost:8081/test/soap/greeting"),
                    containsString("https://localhost:8280/soap/greeting")));
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
