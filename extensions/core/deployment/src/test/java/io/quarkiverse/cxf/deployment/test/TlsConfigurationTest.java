package io.quarkiverse.cxf.deployment.test;

import io.quarkiverse.cxf.QuarkusTLSClientParameters;
import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.tls.CertificateUpdatedEvent;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import jakarta.enterprise.event.Event;
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Certificates(baseDir = "target/classes", //
        certificates = { //
                @Certificate( //
                        name = "localhost", //
                        password = "secret", //
                        formats = { Format.PKCS12 }),
                @Certificate( //
                        name = "localhost2", //
                        password = "secret", //
                        formats = { Format.PKCS12 }),
                @Certificate( //
                        name = "localhost3", //
                        password = "secret", //
                        formats = { Format.PKCS12 }),
        })
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

            /* Named global client TLS configuration */
            .overrideConfigKey("quarkus.tls.client-global-pkcs12.trust-store.p12.path",
                    "target/classes/localhost-truststore.p12")
            .overrideConfigKey("quarkus.tls.client-global-pkcs12.trust-store.p12.password", "secret")

            /* Default Quarkus TLS configuration for the clients */
            .overrideConfigKey("quarkus.tls.trust-store.p12.path", "target/classes/localhost2-truststore.p12")
            .overrideConfigKey("quarkus.tls.trust-store.p12.password", "secret")

            /* Named per client TLS configuration */
            .overrideConfigKey("quarkus.tls.client-pkcs12.trust-store.p12.path", "target/classes/localhost3-truststore.p12")
            .overrideConfigKey("quarkus.tls.client-pkcs12.trust-store.p12.password", "secret")

            /* Client global TLS configuration */
            .overrideConfigKey("quarkus.cxf.client.tls-configuration-name", "client-global-pkcs12")

            /*
             * Client with VertxHttpClientHTTPConduitFactory
             * Clients 1 and 2 test the client global named TLS configuration
             * Clients 3 and 4 test the default Quarkus TLS configuration
             * Clients 5 and 6 test the per client named TLS configuration
             */
            .overrideConfigKey("quarkus.cxf.client.helloVertx.client-endpoint-url", "https://localhost:8444/services/hello")
            .overrideConfigKey("quarkus.cxf.client.helloVertx.logging.enabled", "true")
            .overrideConfigKey("quarkus.cxf.client.helloVertx.service-interface", HelloService.class.getName())
            .overrideConfigKey("quarkus.cxf.client.helloVertx.http-conduit-factory", "VertxHttpClientHTTPConduitFactory")

            .overrideConfigKey("quarkus.cxf.client.helloVertx2.client-endpoint-url", "https://localhost:8444/services/hello")
            .overrideConfigKey("quarkus.cxf.client.helloVertx2.logging.enabled", "true")
            .overrideConfigKey("quarkus.cxf.client.helloVertx2.service-interface", HelloService.class.getName())
            .overrideConfigKey("quarkus.cxf.client.helloVertx2.http-conduit-factory", "VertxHttpClientHTTPConduitFactory")

            .overrideConfigKey("quarkus.cxf.client.helloVertx3.client-endpoint-url", "https://localhost:8444/services/hello")
            .overrideConfigKey("quarkus.cxf.client.helloVertx3.logging.enabled", "true")
            .overrideConfigKey("quarkus.cxf.client.helloVertx3.service-interface", HelloService.class.getName())
            .overrideConfigKey("quarkus.cxf.client.helloVertx3.http-conduit-factory", "VertxHttpClientHTTPConduitFactory")
            .overrideConfigKey("quarkus.cxf.client.helloVertx3.tls-configuration-name", "<default>")

            .overrideConfigKey("quarkus.cxf.client.helloVertx4.client-endpoint-url", "https://localhost:8444/services/hello")
            .overrideConfigKey("quarkus.cxf.client.helloVertx4.logging.enabled", "true")
            .overrideConfigKey("quarkus.cxf.client.helloVertx4.service-interface", HelloService.class.getName())
            .overrideConfigKey("quarkus.cxf.client.helloVertx4.http-conduit-factory", "VertxHttpClientHTTPConduitFactory")
            .overrideConfigKey("quarkus.cxf.client.helloVertx4.tls-configuration-name", "<default>")

            .overrideConfigKey("quarkus.cxf.client.helloVertx5.client-endpoint-url", "https://localhost:8444/services/hello")
            .overrideConfigKey("quarkus.cxf.client.helloVertx5.logging.enabled", "true")
            .overrideConfigKey("quarkus.cxf.client.helloVertx5.service-interface", HelloService.class.getName())
            .overrideConfigKey("quarkus.cxf.client.helloVertx5.http-conduit-factory", "VertxHttpClientHTTPConduitFactory")
            .overrideConfigKey("quarkus.cxf.client.helloVertx5.tls-configuration-name", "client-pkcs12")

            .overrideConfigKey("quarkus.cxf.client.helloVertx6.client-endpoint-url", "https://localhost:8444/services/hello")
            .overrideConfigKey("quarkus.cxf.client.helloVertx6.logging.enabled", "true")
            .overrideConfigKey("quarkus.cxf.client.helloVertx6.service-interface", HelloService.class.getName())
            .overrideConfigKey("quarkus.cxf.client.helloVertx6.http-conduit-factory", "VertxHttpClientHTTPConduitFactory")
            .overrideConfigKey("quarkus.cxf.client.helloVertx6.tls-configuration-name", "client-pkcs12")

            /* Client with HttpClientHTTPConduitFactory */
            .overrideConfigKey("quarkus.cxf.client.helloHttpClient.client-endpoint-url",
                    "https://localhost:8444/services/hello")
            .overrideConfigKey("quarkus.cxf.client.helloHttpClient.logging.enabled", "true")
            .overrideConfigKey("quarkus.cxf.client.helloHttpClient.service-interface", HelloService.class.getName())
            .overrideConfigKey("quarkus.cxf.client.helloHttpClient.http-conduit-factory", "HttpClientHTTPConduitFactory")

            .overrideConfigKey("quarkus.cxf.client.helloHttpClient2.client-endpoint-url",
                    "https://localhost:8444/services/hello")
            .overrideConfigKey("quarkus.cxf.client.helloHttpClient2.logging.enabled", "true")
            .overrideConfigKey("quarkus.cxf.client.helloHttpClient2.service-interface", HelloService.class.getName())
            .overrideConfigKey("quarkus.cxf.client.helloHttpClient2.http-conduit-factory", "HttpClientHTTPConduitFactory")
            .overrideConfigKey("quarkus.cxf.client.helloHttpClient2.tls-configuration-name", "<default>")

            .overrideConfigKey("quarkus.cxf.client.helloHttpClient3.client-endpoint-url",
                    "https://localhost:8444/services/hello")
            .overrideConfigKey("quarkus.cxf.client.helloHttpClient3.logging.enabled", "true")
            .overrideConfigKey("quarkus.cxf.client.helloHttpClient3.service-interface", HelloService.class.getName())
            .overrideConfigKey("quarkus.cxf.client.helloHttpClient3.http-conduit-factory", "HttpClientHTTPConduitFactory")
            .overrideConfigKey("quarkus.cxf.client.helloHttpClient3.tls-configuration-name", "client-pkcs12")

            /* Client with URLConnectionHTTPConduitFactory */
            .overrideConfigKey("quarkus.cxf.client.helloUrlConnection.client-endpoint-url",
                    "https://localhost:8444/services/hello")
            .overrideConfigKey("quarkus.cxf.client.helloUrlConnection.logging.enabled", "true")
            .overrideConfigKey("quarkus.cxf.client.helloUrlConnection.service-interface", HelloService.class.getName())
            .overrideConfigKey("quarkus.cxf.client.helloUrlConnection.http-conduit-factory", "URLConnectionHTTPConduitFactory")

            .overrideConfigKey("quarkus.cxf.client.helloUrlConnection2.client-endpoint-url",
                    "https://localhost:8444/services/hello")
            .overrideConfigKey("quarkus.cxf.client.helloUrlConnection2.logging.enabled", "true")
            .overrideConfigKey("quarkus.cxf.client.helloUrlConnection2.service-interface", HelloService.class.getName())
            .overrideConfigKey("quarkus.cxf.client.helloUrlConnection2.http-conduit-factory", "URLConnectionHTTPConduitFactory")
            .overrideConfigKey("quarkus.cxf.client.helloUrlConnection2.tls-configuration-name", "<default>")

            .overrideConfigKey("quarkus.cxf.client.helloUrlConnection3.client-endpoint-url",
                    "https://localhost:8444/services/hello")
            .overrideConfigKey("quarkus.cxf.client.helloUrlConnection3.logging.enabled", "true")
            .overrideConfigKey("quarkus.cxf.client.helloUrlConnection3.service-interface", HelloService.class.getName())
            .overrideConfigKey("quarkus.cxf.client.helloUrlConnection3.http-conduit-factory", "URLConnectionHTTPConduitFactory")
            .overrideConfigKey("quarkus.cxf.client.helloUrlConnection3.tls-configuration-name", "client-pkcs12");

    @CXFClient("helloVertx")
    HelloService helloVertx;

    @CXFClient("helloVertx2")
    HelloService helloVertx2;

    @CXFClient("helloVertx3")
    HelloService helloVertx3;

    @CXFClient("helloVertx4")
    HelloService helloVertx4;

    @CXFClient("helloVertx5")
    HelloService helloVertx5;

    @CXFClient("helloVertx6")
    HelloService helloVertx6;

    @CXFClient("helloHttpClient")
    HelloService helloHttpClient;

    @CXFClient("helloHttpClient2")
    HelloService helloHttpClient2;

    @CXFClient("helloHttpClient3")
    HelloService helloHttpClient3;

    @CXFClient("helloUrlConnection")
    HelloService helloUrlConnection;

    @CXFClient("helloUrlConnection2")
    HelloService helloUrlConnection2;

    @CXFClient("helloUrlConnection3")
    HelloService helloUrlConnection3;

    @Inject
    TlsConfigurationRegistry registry;

    @Inject
    Event<CertificateUpdatedEvent> event;

    @Inject
    Logger logger;

    static final Path localHostKs = Path.of("target/classes/localhost-keystore.p12");
    static final Path localHostKsCp = Path.of("target/classes/localhost-keystore-cp.p12");
    static final Path localHostKs2 = Path.of("target/classes/localhost2-keystore.p12");
    static final Path localHostKs3 = Path.of("target/classes/localhost3-keystore.p12");

    @Test
    void vertxJVMDefault() {
        Assertions.assertThat(helloVertx.hello("Doe")).isEqualTo("Hello Doe");
    }

    @Test
    void vertxQuarkusDefault() {
        changeKeystore(localHostKs2);
        Assertions.assertThat(helloVertx3.hello("Doe")).isEqualTo("Hello Doe");
        restoreKeystore();
    }

    @Test
    void vertxNamed() {
        changeKeystore(localHostKs3);
        Assertions.assertThat(helloVertx5.hello("Doe")).isEqualTo("Hello Doe");
        restoreKeystore();
    }

    @Test
    void httpClientJVMDefault() {
        Assertions.assertThat(helloHttpClient.hello("Doe")).isEqualTo("Hello Doe");
    }

    @Test
    void httpClientQuarkusDefault() {
        changeKeystore(localHostKs2);
        Assertions.assertThat(helloHttpClient2.hello("Doe")).isEqualTo("Hello Doe");
        restoreKeystore();
    }

    @Test
    void httpClientNamed() {
        changeKeystore(localHostKs3);
        Assertions.assertThat(helloHttpClient3.hello("Doe")).isEqualTo("Hello Doe");
        restoreKeystore();
    }

    @Test
    void urlConnectionJVMDefault() {
        Assertions.assertThat(helloUrlConnection.hello("Doe")).isEqualTo("Hello Doe");
    }

    @Test
    void urlConnectionQuarkusDefault() {
        changeKeystore(localHostKs2);
        Assertions.assertThat(helloUrlConnection2.hello("Doe")).isEqualTo("Hello Doe");
        restoreKeystore();
    }

    @Test
    void urlConnectionNamed() {
        changeKeystore(localHostKs3);
        Assertions.assertThat(helloUrlConnection3.hello("Doe")).isEqualTo("Hello Doe");
        restoreKeystore();
    }

    @Test
    void sameTlsConfiguration() {

        /*
         * The TlsConfigurations must be the same instance, otherwise the identity based caching in HttpClientPool would not
         * work
         */

        TLSClientParameters p1 = getTLSClientParameters(helloVertx);
        TLSClientParameters p2 = getTLSClientParameters(helloVertx2);
        TLSClientParameters p3 = getTLSClientParameters(helloVertx3);
        TLSClientParameters p4 = getTLSClientParameters(helloVertx4);
        TLSClientParameters p5 = getTLSClientParameters(helloVertx5);
        TLSClientParameters p6 = getTLSClientParameters(helloVertx6);

        Assertions.assertThat(p1).isNotNull().isInstanceOf(QuarkusTLSClientParameters.class);
        Assertions.assertThat(p2).isNotNull().isInstanceOf(QuarkusTLSClientParameters.class);
        Assertions.assertThat(p3).isNotNull().isInstanceOf(QuarkusTLSClientParameters.class);
        Assertions.assertThat(p4).isNotNull().isInstanceOf(QuarkusTLSClientParameters.class);
        Assertions.assertThat(p5).isNotNull().isInstanceOf(QuarkusTLSClientParameters.class);
        Assertions.assertThat(p6).isNotNull().isInstanceOf(QuarkusTLSClientParameters.class);
        Assertions.assertThat(((QuarkusTLSClientParameters) p1).getTlsConfiguration())
                .isSameAs(((QuarkusTLSClientParameters) p2).getTlsConfiguration());
        Assertions.assertThat(((QuarkusTLSClientParameters) p3).getTlsConfiguration())
                .isSameAs(((QuarkusTLSClientParameters) p4).getTlsConfiguration());
        Assertions.assertThat(((QuarkusTLSClientParameters) p5).getTlsConfiguration())
                .isSameAs(((QuarkusTLSClientParameters) p6).getTlsConfiguration());
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

    private void changeKeystore(Path newKeystore) {
        try {
            Files.copy(localHostKs, localHostKsCp, StandardCopyOption.REPLACE_EXISTING);
            Files.copy(newKeystore, localHostKs, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        reload();
    }

    void restoreKeystore() {
        try {
            Files.copy(localHostKsCp, localHostKs, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        reload();
    }

    private void reload() {
        final TlsConfiguration c = registry.get("localhost-pkcs12").get();
        Assertions.assertThat(c.reload()).isTrue();
        event.fire(new CertificateUpdatedEvent("localhost-pkcs12", c));
    }

    @WebService(serviceName = "HelloService")
    public static class HelloServiceImpl implements HelloService {

        @Override
        public String hello(String person) {
            return "Hello " + person;
        }
    }

}
