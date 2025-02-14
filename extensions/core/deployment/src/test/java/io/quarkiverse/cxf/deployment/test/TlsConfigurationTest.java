package io.quarkiverse.cxf.deployment.test;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.security.KeyStore;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.inject.Instance;
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
import org.junit.jupiter.api.AfterAll;
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

            /* Default Quarkus TLS configuration for the clients */
            .overrideConfigKey("quarkus.tls.trust-store.p12.path", "target/classes/localhost-truststore.p12")
            .overrideConfigKey("quarkus.tls.trust-store.p12.password", "secret")

            /* Named TLS configuration for the clients */
            .overrideConfigKey("quarkus.tls.client-pkcs12.trust-store.p12.path", "target/classes/localhost-truststore.p12")
            .overrideConfigKey("quarkus.tls.client-pkcs12.trust-store.p12.password", "secret")

            /*
             * Client with VertxHttpClientHTTPConduitFactory
             * Clients 1 and 2 test the java.net.ssl.trustStore
             * Clients 3 and 4 test the default Quarkus TLS configurations
             * Clients 5 and 6 test the named TLS configurations
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

    HelloService helloVertx;

    HelloService helloVertx2;

    @CXFClient("helloVertx")
    Instance<HelloService> helloVertxInstance;

    @CXFClient("helloVertx2")
    Instance<HelloService> helloVertxInstance2;

    @CXFClient("helloVertx3")
    HelloService helloVertx3;

    @CXFClient("helloVertx4")
    HelloService helloVertx4;

    @CXFClient("helloVertx5")
    HelloService helloVertx5;

    @CXFClient("helloVertx6")
    HelloService helloVertx6;

    HelloService helloHttpClient;

    @CXFClient("helloHttpClient")
    Instance<HelloService> helloHttpClientInstance;

    @CXFClient("helloHttpClient2")
    HelloService helloHttpClient2;

    @CXFClient("helloHttpClient3")
    HelloService helloHttpClient3;

    HelloService helloUrlConnection;

    @CXFClient("helloUrlConnection")
    Instance<HelloService> helloUrlConnectionInstance;

    @CXFClient("helloUrlConnection2")
    HelloService helloUrlConnection2;

    @CXFClient("helloUrlConnection3")
    HelloService helloUrlConnection3;

    @Inject
    Logger logger;

    @PostConstruct
    void setup() throws Exception {
        addCertToDefaultCacert();
        this.helloVertx = helloVertxInstance.get();
        this.helloVertx2 = helloVertxInstance2.get();
        this.helloHttpClient = helloHttpClientInstance.get();
        this.helloUrlConnection = helloUrlConnectionInstance.get();
    }

    @AfterAll
    public static void cleanup() throws Exception {
        removeCertFromDefaultCacert();
    }

    private static void addCertToDefaultCacert() throws Exception {
        String cacertsPath = System.getProperty("java.home") + "/lib/security/cacerts";
        String cacertsPassword = "changeit";

        KeyStore cacerts = KeyStore.getInstance(KeyStore.getDefaultType());
        try (FileInputStream cacertsInput = new FileInputStream(cacertsPath)) {
            cacerts.load(cacertsInput, cacertsPassword.toCharArray());
        }

        Path p12FilePath = Path.of("target/classes/localhost-truststore.p12");
        String p12Password = "secret";
        KeyStore p12Store = KeyStore.getInstance("PKCS12");
        try (FileInputStream p12Input = new FileInputStream(p12FilePath.toFile())) {
            p12Store.load(p12Input, p12Password.toCharArray());
        }

        java.security.cert.Certificate cert = p12Store.getCertificate("localhost");
        cacerts.setCertificateEntry("localhost", cert);

        try (FileOutputStream cacertsOutput = new FileOutputStream(cacertsPath)) {
            cacerts.store(cacertsOutput, cacertsPassword.toCharArray());
        }

        System.out.println("Successfully updated cacerts with certificates from the P12 file.");
    }

    public static void removeCertFromDefaultCacert() throws Exception {
        String cacertsPath = System.getProperty("java.home") + "/lib/security/cacerts";
        String cacertsPassword = "changeit";

        KeyStore cacerts = KeyStore.getInstance(KeyStore.getDefaultType());
        try (FileInputStream cacertsInput = new FileInputStream(cacertsPath)) {
            cacerts.load(cacertsInput, cacertsPassword.toCharArray());
        }

        if (cacerts.containsAlias("localhost")) {
            cacerts.deleteEntry("localhost");

            try (FileOutputStream cacertsOutput = new FileOutputStream(cacertsPath)) {
                cacerts.store(cacertsOutput, cacertsPassword.toCharArray());
            }
            System.out.println("Removed certificate with alias localhost");
        }
    }

    @Test
    void vertxJVMDefault() {
        Assertions.assertThat(helloVertx.hello("Doe")).isEqualTo("Hello Doe");
    }

    @Test
    void vertxQuarkusDefault() {
        Assertions.assertThat(helloVertx3.hello("Doe")).isEqualTo("Hello Doe");
    }

    @Test
    void vertxNamed() {
        Assertions.assertThat(helloVertx5.hello("Doe")).isEqualTo("Hello Doe");
    }

    @Test
    void httpClientJVMDefault() {
        Assertions.assertThat(helloHttpClient.hello("Doe")).isEqualTo("Hello Doe");
    }

    @Test
    void httpClientQuarkusDefault() {
        Assertions.assertThat(helloHttpClient2.hello("Doe")).isEqualTo("Hello Doe");
    }

    @Test
    void httpClientNamed() {
        Assertions.assertThat(helloHttpClient3.hello("Doe")).isEqualTo("Hello Doe");
    }

    @Test
    void urlConnectionJVMDefault() {
        Assertions.assertThat(helloUrlConnection.hello("Doe")).isEqualTo("Hello Doe");
    }

    @Test
    void urlConnectionQuarkusDefault() {
        Assertions.assertThat(helloUrlConnection2.hello("Doe")).isEqualTo("Hello Doe");
    }

    @Test
    void urlConnectionNamed() {
        Assertions.assertThat(helloUrlConnection3.hello("Doe")).isEqualTo("Hello Doe");
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

    @WebService(serviceName = "HelloService")
    public static class HelloServiceImpl implements HelloService {

        @Override
        public String hello(String person) {
            return "Hello " + person;
        }
    }

}
