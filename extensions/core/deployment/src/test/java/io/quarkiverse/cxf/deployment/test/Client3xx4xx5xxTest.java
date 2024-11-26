package io.quarkiverse.cxf.deployment.test;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.transport.http.URLConnectionHTTPConduit;
import org.assertj.core.api.Assertions;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkus.logging.Log;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class Client3xx4xx5xxTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(HelloService.class, HelloServiceImpl.class))

            /* Service */
            .overrideConfigKey("quarkus.cxf.endpoint.\"/hello\".implementor",
                    HelloServiceImpl.class.getName())
            .overrideConfigKey("quarkus.cxf.endpoint.\"/hello\".logging.enabled", "true")

            /* Clients */
            .overrideConfigKey("quarkus.cxf.client.wsdlUri200.client-endpoint-url", "http://localhost:8081/services/hello")
            .overrideConfigKey("quarkus.cxf.client.wsdlUri200.wsdl", "http://localhost:8081/services/hello?wsdl")
            // Not needed when the WSDL is set and HelloService has both serviceName and targetNamespace set
            //.overrideConfigKey("quarkus.cxf.client.wsdlUri404.service-interface", HelloService.class.getName())
            .overrideConfigKey("quarkus.cxf.client.wsdlUri200.logging.enabled", "true")

            /* Bad WSDL URI */
            .overrideConfigKey("quarkus.cxf.client.wsdlUri404.client-endpoint-url", "http://localhost:8081/services/hello")
            .overrideConfigKey("quarkus.cxf.client.wsdlUri404.wsdl", "http://localhost:8081/services/no-such-service?wsdl")
            .overrideConfigKey("quarkus.cxf.client.wsdlUri404.logging.enabled", "true")

            /* Bad service endpoint URI */
            .overrideConfigKey("quarkus.cxf.client.endpointUri404.client-endpoint-url",
                    "http://localhost:8081/services/no-such-service")
            .overrideConfigKey("quarkus.cxf.client.endpointUri404.service-interface", HelloService.class.getName())
            .overrideConfigKey("quarkus.cxf.client.endpointUri404.logging.enabled", "true")

            /* Bad service endpoint URI */
            .overrideConfigKey("quarkus.cxf.client.endpointUri302.client-endpoint-url",
                    "http://localhost:8081/vertx-redirect")
            .overrideConfigKey("quarkus.cxf.client.endpointUri302.service-interface", HelloService.class.getName())
            .overrideConfigKey("quarkus.cxf.client.endpointUri302.auto-redirect", "true")
            .overrideConfigKey("quarkus.cxf.client.endpointUri302.logging.enabled", "true");

    @CXFClient("wsdlUri200")
    // Use Instance to avoid greedy initialization
    Instance<HelloService> wsdlUri200;

    @CXFClient("wsdlUri404")
    Instance<HelloService> wsdlUri404;

    @CXFClient("endpointUri404")
    Instance<HelloService> endpointUri404;

    @CXFClient("endpointUri302")
    Instance<HelloService> endpointUri302;

    Instance<HelloService> getClient(String clientName) {
        switch (clientName) {
            case "wsdlUri200": {
                return wsdlUri200;
            }
            case "wsdlUri404": {
                return wsdlUri404;
            }
            case "endpointUri404": {
                return endpointUri404;
            }
            case "endpointUri302": {
                return endpointUri302;
            }
            default:
                throw new IllegalArgumentException("Unexpected client name: " + clientName);
        }
    }

    @Test
    void wsdlUri200() {
        Assertions.assertThat(wsdlUri200.get().hello("foo")).isEqualTo("Hello foo");
    }

    @Test
    void wsdlUri404() {
        Assertions.assertThatThrownBy(() -> wsdlUri404.get().hello("foo"))
                .hasRootCauseInstanceOf(org.apache.cxf.transport.http.HTTPException.class)
                .hasRootCauseMessage(
                        "HTTP response '404: Not Found' when communicating with http://localhost:8081/services/no-such-service?wsdl");
    }

    @Test
    void endpointUri404() {
        Assertions.assertThatThrownBy(() -> endpointUri404.get().hello("foo")).hasRootCauseMessage(
                "HTTP response '404: Not Found' when communicating with http://localhost:8081/services/no-such-service");
    }

    void init(@Observes Router router) {
        router.route().handler(BodyHandler.create());
        router.post("/vertx-blocking/:client").blockingHandler(ctx -> {
            final String person = ctx.body().asString();
            final String resp = getClient(ctx.pathParam("client")).get().hello(person);
            ctx.response().end(resp);
        });
        router.post("/vertx/:client").handler(ctx -> {
            final String person = ctx.body().asString();
            try {
                final String resp = getClient(ctx.pathParam("client")).get().hello(person);
                ctx.response().end(resp);
            } catch (Exception e) {
                Throwable r = rootCause(e);
                ctx.response().setStatusCode(500).end(r.getClass().getName() + " " + r.getMessage());
            }
        });
        router.post("/vertx-redirect").handler(ctx -> {
            Log.info("Redirecting");
            ctx.redirect("http://localhost:8081/services/hello");
        });
    }

    @Test
    void wsdlUri200OnWorkerThread() {
        RestAssured.given()
                .body("Joe")
                .post("http://localhost:8081/vertx-blocking/wsdlUri200")
                .then()
                .statusCode(200)
                .body(Matchers.is("Hello Joe"));
    }

    @Test
    void wsdlUri200OnEventLoop() throws InterruptedException {
        final Client client = ClientProxy.getClient(wsdlUri200.get());
        if (client.getConduit() instanceof URLConnectionHTTPConduit) {
            /* URLConnectionHTTPConduit is not as picky as VertxHttpClientHTTPConduit */
            RestAssured.given()
                    .body("Joe")
                    .post("http://localhost:8081/vertx/wsdlUri200")
                    .then()
                    .statusCode(200)
                    .body(Matchers.is("Hello Joe"));
        } else {
            /* VertxHttpClientHTTPConduit */
            RestAssured.given()
                    .body("Joe")
                    .post("http://localhost:8081/vertx/wsdlUri200")
                    .then()
                    .statusCode(500)
                    .body(CoreMatchers.containsString(
                            "java.lang.IllegalStateException You have attempted to perform a blocking operation on an IO thread."));
        }

    }

    @Test
    void endpointUri404OnWorkerThread() {
        RestAssured.given()
                .body("Joe")
                .post("http://localhost:8081/vertx-blocking/endpointUri404")
                .then()
                .statusCode(500)
                .body(CoreMatchers.containsString(
                        "org.apache.cxf.transport.http.HTTPException: HTTP response '404: Not Found' when communicating with http://localhost:8081/services/no-such-service"));
    }

    @Test
    void endpointUri404OnEventLoop() throws InterruptedException {
        final Client client = ClientProxy.getClient(endpointUri404.get());
        if (client.getConduit() instanceof URLConnectionHTTPConduit) {
            /* URLConnectionHTTPConduit is not as picky as VertxHttpClientHTTPConduit */
            RestAssured.given()
                    .body("Joe")
                    .post("http://localhost:8081/vertx/endpointUri404")
                    .then()
                    .statusCode(500)
                    .body(CoreMatchers.containsString(
                            "org.apache.cxf.transport.http.HTTPException HTTP response '404: Not Found' when communicating with http://localhost:8081/services/no-such-service"));
        } else {
            /* VertxHttpClientHTTPConduit */
            RestAssured.given()
                    .body("Joe")
                    .post("http://localhost:8081/vertx/endpointUri404")
                    .then()
                    .statusCode(500)
                    .body(CoreMatchers.containsString(
                            "java.lang.IllegalStateException You have attempted to perform a blocking operation on an IO thread."));

        }

    }

    @Test
    void endpointUri302OnWorkerThread() {
        RestAssured.given()
                .body("Joe")
                .post("http://localhost:8081/vertx-blocking/endpointUri302")
                .then()
                .statusCode(200)
                .body(Matchers.is("Hello Joe"));
    }

    private static Throwable rootCause(Exception e) {
        Throwable result = e;
        while (result.getCause() != null) {
            result = result.getCause();
        }
        return result;
    }

    @WebService(serviceName = "HelloService", targetNamespace = "http://test.deployment.cxf.quarkiverse.io/")
    public interface HelloService {

        @WebMethod
        String hello(String person);

    }

    @WebService(serviceName = "HelloService")
    public static class HelloServiceImpl implements HelloService {

        @Override
        public String hello(String person) {
            return "Hello " + person;
        }
    }

}
