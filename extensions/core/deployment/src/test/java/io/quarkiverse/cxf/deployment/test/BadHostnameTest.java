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
                name = "fake-host", //
                password = "secret", //
                cn = "fake-host", //
                formats = { Format.PKCS12 }))
public class BadHostnameTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(HelloService.class, HelloServiceImpl.class))

            /* Server TLS */
            .overrideConfigKey("quarkus.tls.key-store.p12.path", "fake-host-keystore.p12")
            .overrideConfigKey("quarkus.tls.key-store.p12.password", "secret")
            .overrideConfigKey("quarkus.http.insecure-requests", "disabled")
            /* Service */
            .overrideConfigKey("quarkus.cxf.endpoint.\"/hello\".implementor",
                    HelloServiceImpl.class.getName())
            .overrideConfigKey("quarkus.cxf.endpoint.\"/hello\".logging.enabled", "true")

            /* Named TLS configuration for the clients */
            .overrideConfigKey("quarkus.tls.client-pkcs12.trust-store.p12.path", "fake-host-truststore.p12")
            .overrideConfigKey("quarkus.tls.client-pkcs12.trust-store.p12.password", "secret")
            .overrideConfigKey("quarkus.tls.client-pkcs12.trust-store.certificate-expiration-policy", "IGNORE")

            /* Client with VertxHttpClientHTTPConduitFactory */
            .overrideConfigKey("quarkus.cxf.client.helloVertx.client-endpoint-url", "https://localhost:8444/services/hello")
            .overrideConfigKey("quarkus.cxf.client.helloVertx.logging.enabled", "true")
            .overrideConfigKey("quarkus.cxf.client.helloVertx.service-interface", HelloService.class.getName())
            .overrideConfigKey("quarkus.cxf.client.helloVertx.http-conduit-factory", "VertxHttpClientHTTPConduitFactory")
            .overrideConfigKey("quarkus.cxf.client.helloVertx.tls-configuration-name", "client-pkcs12")

            /* Client with HttpClientHTTPConduitFactory */
            .overrideConfigKey("quarkus.cxf.client.helloHttpClient.client-endpoint-url",
                    "https://localhost:8444/services/hello")
            .overrideConfigKey("quarkus.cxf.client.helloHttpClient.logging.enabled", "true")
            .overrideConfigKey("quarkus.cxf.client.helloHttpClient.service-interface", HelloService.class.getName())
            .overrideConfigKey("quarkus.cxf.client.helloHttpClient.http-conduit-factory", "HttpClientHTTPConduitFactory")
            .overrideConfigKey("quarkus.cxf.client.helloHttpClient.tls-configuration-name", "client-pkcs12")

            /* Client with URLConnectionHTTPConduitFactory */
            .overrideConfigKey("quarkus.cxf.client.helloUrlConnection.client-endpoint-url",
                    "https://localhost:8444/services/hello")
            .overrideConfigKey("quarkus.cxf.client.helloUrlConnection.logging.enabled", "true")
            .overrideConfigKey("quarkus.cxf.client.helloUrlConnection.service-interface", HelloService.class.getName())
            .overrideConfigKey("quarkus.cxf.client.helloUrlConnection.http-conduit-factory", "URLConnectionHTTPConduitFactory")
            .overrideConfigKey("quarkus.cxf.client.helloUrlConnection.tls-configuration-name", "client-pkcs12");

    @CXFClient("helloVertx")
    HelloService helloVertx;

    @CXFClient("helloHttpClient")
    HelloService helloHttpClient;

    @CXFClient("helloUrlConnection")
    HelloService helloUrlConnection;

    @Inject
    Logger logger;

    @Test
    void vertx() {
        Assertions.assertThatThrownBy(() -> helloVertx.hello("Doe")).hasRootCauseMessage(
                "No subject alternative DNS name matching localhost found.");
    }

    @Test
    void httpClient() {
        Assertions.assertThatThrownBy(() -> helloHttpClient.hello("Doe")).hasRootCauseMessage(
                "No name matching localhost found");
    }

    @Test
    void urlConnection() {
        Assertions.assertThatThrownBy(() -> helloUrlConnection.hello("Doe")).hasRootCauseMessage(
                "The https URL hostname does not match the Common Name (CN) on the server certificate in the client's truststore.  Make sure server certificate is correct, or to disable this check (NOT recommended for production) set the CXF client TLS configuration property \"disableCNCheck\" to true.");
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
