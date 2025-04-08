package io.quarkiverse.cxf.deployment.test;

import jakarta.inject.Inject;
import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.transport.http.HTTPConduit;
import org.assertj.core.api.Assertions;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.cxf.QuarkusTLSClientParameters;
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
public class TlsConfigurationTest {

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

            /* Client with VertxHttpClientHTTPConduitFactory */
            .overrideConfigKey("quarkus.cxf.client.helloVertx.client-endpoint-url", "https://localhost:8444/services/hello")
            .overrideConfigKey("quarkus.cxf.client.helloVertx.logging.enabled", "true")
            .overrideConfigKey("quarkus.cxf.client.helloVertx.service-interface", HelloService.class.getName())
            .overrideConfigKey("quarkus.cxf.client.helloVertx.http-conduit-factory", "VertxHttpClientHTTPConduitFactory")
            .overrideConfigKey("quarkus.cxf.client.helloVertx.tls-configuration-name", "client-pkcs12")

            .overrideConfigKey("quarkus.cxf.client.helloVertx2.client-endpoint-url", "https://localhost:8444/services/hello")
            .overrideConfigKey("quarkus.cxf.client.helloVertx2.logging.enabled", "true")
            .overrideConfigKey("quarkus.cxf.client.helloVertx2.service-interface", HelloService.class.getName())
            .overrideConfigKey("quarkus.cxf.client.helloVertx2.http-conduit-factory", "VertxHttpClientHTTPConduitFactory")
            .overrideConfigKey("quarkus.cxf.client.helloVertx2.tls-configuration-name", "client-pkcs12")

            /* Client with URLConnectionHTTPConduitFactory */
            .overrideConfigKey("quarkus.cxf.client.helloUrlConnection.client-endpoint-url",
                    "https://localhost:8444/services/hello")
            .overrideConfigKey("quarkus.cxf.client.helloUrlConnection.logging.enabled", "true")
            .overrideConfigKey("quarkus.cxf.client.helloUrlConnection.service-interface", HelloService.class.getName())
            .overrideConfigKey("quarkus.cxf.client.helloUrlConnection.http-conduit-factory", "URLConnectionHTTPConduitFactory")
            .overrideConfigKey("quarkus.cxf.client.helloUrlConnection.tls-configuration-name", "client-pkcs12");

    @CXFClient("helloVertx")
    HelloService helloVertx;

    @CXFClient("helloVertx2")
    HelloService helloVertx2;

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

    @Test
    void sameTlsConfiguration() {

        /*
         * The TlsConfigurations must be the same instance, otherwise the identity based caching in HttpClientPool would not
         * work
         */

        TLSClientParameters p1 = getTLSClientParameters(helloVertx2);
        TLSClientParameters p2 = getTLSClientParameters(helloVertx);

        Assertions.assertThat(p1).isNotNull().isInstanceOf(QuarkusTLSClientParameters.class);
        Assertions.assertThat(p2).isNotNull().isInstanceOf(QuarkusTLSClientParameters.class);
        Assertions.assertThat(((QuarkusTLSClientParameters) p1).getTlsConfiguration())
                .isSameAs(((QuarkusTLSClientParameters) p2).getTlsConfiguration());

    }

    static TLSClientParameters getTLSClientParameters(HelloService cl) {
        final Client client = ClientProxy.getClient(cl);
        HTTPConduit httpConduit = (HTTPConduit) client.getConduit();
        return httpConduit.getTlsClientParameters();
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
