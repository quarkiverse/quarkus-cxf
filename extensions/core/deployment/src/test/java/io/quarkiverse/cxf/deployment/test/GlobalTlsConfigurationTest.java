package io.quarkiverse.cxf.deployment.test;

import jakarta.inject.Inject;
import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

import org.assertj.core.api.Assertions;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;

@Certificates(baseDir = "target/classes", //
        certificates = @Certificate( //
                name = "localhost", //
                password = "secret", //
                formats = { Format.PKCS12 }))
public class GlobalTlsConfigurationTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(HelloService.class, HelloServiceImpl.class))

            /* Server TLS */
            .overrideConfigKey("quarkus.tls.localhost-pkcs12.key-store.p12.path", "localhost-keystore.p12")
            .overrideConfigKey("quarkus.tls.localhost-pkcs12.key-store.p12.password", "secret")
            .overrideConfigKey("quarkus.http.tls-configuration-name", "localhost-pkcs12")
            .overrideConfigKey("quarkus.http.insecure-requests", "disabled")
            /* Service */
            .overrideConfigKey("quarkus.cxf.endpoint.\"/hello\".implementor",
                    HelloServiceImpl.class.getName())
            .overrideConfigKey("quarkus.cxf.endpoint.\"/hello\".logging.enabled", "true")

            /* Named TLS configuration for the clients */
            .overrideConfigKey("quarkus.tls.client-pkcs12.trust-store.p12.path", "target/classes/localhost-truststore.p12")
            .overrideConfigKey("quarkus.tls.client-pkcs12.trust-store.p12.password", "secret")
            /* Global TLS config name for all clients */
            .overrideConfigKey("quarkus.cxf.client.tls-configuration-name", "client-pkcs12")

            /* Client with VertxHttpClientHTTPConduitFactory */
            .overrideConfigKey("quarkus.cxf.client.helloVertx.client-endpoint-url", "https://localhost:8444/services/hello")
            .overrideConfigKey("quarkus.cxf.client.helloVertx.logging.enabled", "true")
            .overrideConfigKey("quarkus.cxf.client.helloVertx.service-interface", HelloService.class.getName())
            .overrideConfigKey("quarkus.cxf.client.helloVertx.http-conduit-factory", "VertxHttpClientHTTPConduitFactory")

            /* Client with URLConnectionHTTPConduitFactory */
            .overrideConfigKey("quarkus.cxf.client.helloUrlConnection.client-endpoint-url",
                    "https://localhost:8444/services/hello")
            .overrideConfigKey("quarkus.cxf.client.helloUrlConnection.logging.enabled", "true")
            .overrideConfigKey("quarkus.cxf.client.helloUrlConnection.service-interface", HelloService.class.getName())
            .overrideConfigKey("quarkus.cxf.client.helloUrlConnection.http-conduit-factory", "URLConnectionHTTPConduitFactory");

    @CXFClient("helloVertx")
    HelloService helloVertx;

    @CXFClient("helloUrlConnection")
    HelloService helloUrlConnection;

    @Inject
    Logger logger;

    @Test
    void vertx() {
        Assertions.assertThat(helloVertx.hello("Doe")).isEqualTo("Hello Doe");
    }

    @Test
    void urlConnection() {
        Assertions.assertThat(helloUrlConnection.hello("Doe")).isEqualTo("Hello Doe");
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
