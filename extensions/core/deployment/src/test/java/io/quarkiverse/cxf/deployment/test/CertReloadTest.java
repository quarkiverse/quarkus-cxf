package io.quarkiverse.cxf.deployment.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkus.logging.Log;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.tls.CertificateUpdatedEvent;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.quarkus.tls.runtime.config.TlsConfigUtils;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;

@Certificates(baseDir = "target/classes", //
        certificates = { //
                @Certificate( //
                        name = "localhost", //
                        password = "secret", //
                        formats = { Format.PKCS12 }),
                @Certificate( //
                        name = "fake-host", //
                        password = "secret", //
                        cn = "fake-host", //
                        formats = { Format.PKCS12 })
        })
public class CertReloadTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(HelloService.class, HelloServiceImpl.class))

            /* Server TLS */
            .overrideConfigKey("quarkus.tls.localhost-pkcs12.key-store.p12.path", "target/classes/localhost-keystore.p12")
            .overrideConfigKey("quarkus.tls.localhost-pkcs12.key-store.p12.password", "secret")
            .overrideConfigKey("quarkus.http.tls-configuration-name", "localhost-pkcs12")
            .overrideConfigKey("quarkus.http.insecure-requests", "disabled")

            /* Service */
            .overrideConfigKey("quarkus.cxf.endpoint.\"/hello\".implementor",
                    HelloServiceImpl.class.getName())
            .overrideConfigKey("quarkus.cxf.endpoint.\"/hello\".logging.enabled", "true")

            /* Named TLS configuration for the client */
            .overrideConfigKey("quarkus.tls.client-pkcs12.trust-store.p12.path", "target/classes/localhost-truststore.p12")
            .overrideConfigKey("quarkus.tls.client-pkcs12.trust-store.p12.password", "secret")

            /* Client connecting to a service that closes the connection upon cert reload */
            .overrideConfigKey("quarkus.cxf.client.helloVertice.client-endpoint-url", "https://localhost:8445/vertx/hello")
            .overrideConfigKey("quarkus.cxf.client.helloVertice.logging.enabled", "true")
            .overrideConfigKey("quarkus.cxf.client.helloVertice.service-interface", HelloService.class.getName())
            .overrideConfigKey("quarkus.cxf.client.helloVertice.http-conduit-factory", "VertxHttpClientHTTPConduitFactory")
            .overrideConfigKey("quarkus.cxf.client.helloVertice.tls-configuration-name", "client-pkcs12")

            /* Client connecting to a service that does not close the connection upon cert reload */
            .overrideConfigKey("quarkus.cxf.client.helloVertx.client-endpoint-url", "https://localhost:8444/services/hello")
            .overrideConfigKey("quarkus.cxf.client.helloVertx.logging.enabled", "true")
            .overrideConfigKey("quarkus.cxf.client.helloVertx.service-interface", HelloService.class.getName())
            .overrideConfigKey("quarkus.cxf.client.helloVertx.http-conduit-factory", "VertxHttpClientHTTPConduitFactory")
            .overrideConfigKey("quarkus.cxf.client.helloVertx.tls-configuration-name", "client-pkcs12")

    ;

    @CXFClient("helloVertx")
    HelloService helloVertx;

    @CXFClient("helloVertice")
    HelloService helloVertice;

    @Inject
    TlsConfigurationRegistry registry;

    @Inject
    Event<CertificateUpdatedEvent> event;

    @Inject
    Logger logger;

    @Inject
    Vertx vertx;

    static final Path localHostTs = Path.of("target/classes/localhost-truststore.p12");
    static final Path localHostKs = Path.of("target/classes/localhost-keystore.p12");
    static final Path localHostTsCp = Path.of("target/classes/localhost-truststore-cp.p12");
    static final Path localHostKsCp = Path.of("target/classes/localhost-keystore-cp.p12");
    static final Path fakeHostTs = Path.of("target/classes/fake-host-truststore.p12");
    static final Path fakeHostKs = Path.of("target/classes/fake-host-keystore.p12");

    @Test
    void verticeDeploy() throws IOException, InterruptedException, ExecutionException {

        {
            String deplId = vertx.deployVerticle(new SoapService()).toCompletionStage().toCompletableFuture().get();
            Assertions.assertThat(deplId).isNotNull();

            /* Initial valid stores should work */
            Awaitility.await().atMost(3000, TimeUnit.SECONDS).until(() -> {
                try {
                    return "Hello Joe".equals(helloVertice.hello("Joe"));
                } catch (Exception e) {
                    return false;
                }
            });

            /*
             * As long as the client is connected, the re-validation of the cert would not happen.
             * Stopping the server will force the client to disconnect
             * and we will be able to get the expected certificate CN exception below
             */
            vertx.undeploy(deplId).toCompletionStage().toCompletableFuture().get();
        }
        /* Make sure the server is down */
        assertServerDown();

        /* Put the valid stores aside */
        Files.move(localHostKs, localHostKsCp, StandardCopyOption.REPLACE_EXISTING);
        Files.move(localHostTs, localHostTsCp, StandardCopyOption.REPLACE_EXISTING);

        /* Replace the the initial valid stores with an invalid ones */
        Files.copy(fakeHostTs, localHostTs, StandardCopyOption.REPLACE_EXISTING);
        Files.copy(fakeHostKs, localHostKs, StandardCopyOption.REPLACE_EXISTING);

        /* Reload the configs */
        reload();

        {
            String deplId = vertx.deployVerticle(new SoapService()).toCompletionStage().toCompletableFuture().get();
            Assertions.assertThat(deplId).isNotNull();

            /* Now the client should fail */
            Awaitility.await().atMost(3000, TimeUnit.SECONDS).until(() -> {
                try {
                    helloVertice.hello("Doe");
                    return false;
                } catch (Exception e) {
                    return rootCause(e).getMessage().equals("No subject alternative DNS name matching localhost found.");
                }
            });

            vertx.undeploy(deplId).toCompletionStage().toCompletableFuture().get();

        }
        /* Make sure the server is down */
        assertServerDown();

        /* Revert everything back */
        Files.move(localHostKsCp, localHostKs, StandardCopyOption.REPLACE_EXISTING);
        Files.move(localHostTsCp, localHostTs, StandardCopyOption.REPLACE_EXISTING);
        reload();
        {
            String deplId = vertx.deployVerticle(new SoapService()).toCompletionStage().toCompletableFuture().get();
            Assertions.assertThat(deplId).isNotNull();

            /* ... and it should work again */
            Awaitility.await().atMost(3000, TimeUnit.SECONDS).until(() -> {
                try {
                    return "Hello Joe".equals(helloVertice.hello("Joe"));
                } catch (Exception e) {
                    return false;
                }
            });

            vertx.undeploy(deplId).toCompletionStage().toCompletableFuture().get();

        }
        /* Make sure the server is down */
        assertServerDown();

    }

    private void assertServerDown() {
        Awaitility.await().atMost(3000, TimeUnit.SECONDS).until(() -> {
            try {
                helloVertice.hello("Doe");
                return false;
            } catch (Exception e) {
                return rootCause(e).getMessage().startsWith("Connection refused"); // There is some suffix on Windows
            }
        });
    }

    @Test
    void simple() throws IOException, InterruptedException, ExecutionException {

        /* Initial valid stores should work */
        Assertions.assertThat(helloVertx.hello("Joe")).isEqualTo("Hello Joe");

        /* Put the valid stores aside */
        Files.move(localHostKs, localHostKsCp, StandardCopyOption.REPLACE_EXISTING);
        Files.move(localHostTs, localHostTsCp, StandardCopyOption.REPLACE_EXISTING);

        /* Replace the the initial valid stores with an invalid ones */
        Files.copy(fakeHostTs, localHostTs, StandardCopyOption.REPLACE_EXISTING);
        Files.copy(fakeHostKs, localHostKs, StandardCopyOption.REPLACE_EXISTING);

        /* Reload the configs */
        reload();

        /* Now the client should fail */
        Awaitility.await().atMost(3000, TimeUnit.SECONDS).until(() -> {
            try {
                helloVertx.hello("Doe");
                return false;
            } catch (Exception e) {
                return rootCause(e).getMessage().equals("No subject alternative DNS name matching localhost found.");
            }
        });

        /* Revert everything back */
        Files.move(localHostKsCp, localHostKs, StandardCopyOption.REPLACE_EXISTING);
        Files.move(localHostTsCp, localHostTs, StandardCopyOption.REPLACE_EXISTING);
        reload();

        /* ... and it should work again */
        Awaitility.await().atMost(3000, TimeUnit.SECONDS).until(() -> {
            try {
                return "Hello Joe".equals(helloVertx.hello("Joe"));
            } catch (Exception e) {
                return false;
            }
        });

    }

    static Throwable rootCause(Throwable e) {
        while (e.getCause() != null) {
            e = e.getCause();
        }
        return e;
    }

    void reload() {
        for (String name : new String[] { "localhost-pkcs12", "client-pkcs12" }) {
            final TlsConfiguration c = registry.get(name).get();
            Assertions.assertThat(c.reload()).isTrue();
            event.fire(new CertificateUpdatedEvent(name, c));
        }
    }

    class SoapService extends AbstractVerticle {

        @Override
        public void start() {
            final Router router = Router.router(vertx);
            router.route("/vertx/hello").handler(ctx -> {
                ctx.response()
                        .putHeader("content-type", "text/xml")
                        .end("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><ns2:helloResponse xmlns:ns2=\"http://test.deployment.cxf.quarkiverse.io/\"><return>Hello Joe</return></ns2:helloResponse></soap:Body></soap:Envelope>");
            });

            final HttpServerOptions opts = new HttpServerOptions();
            TlsConfigUtils.configure(opts, registry.get("localhost-pkcs12").get());

            int port = 8445;
            vertx.createHttpServer(opts)
                    .requestHandler(router)
                    .listen(port, http -> {
                        if (http.succeeded()) {
                            Log.info("HTTP server started on port " + port);
                        } else {
                            Log.errorf(http.cause(), "Failed to start HTTP server");
                        }
                    });
        }
    }

    @WebService
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
