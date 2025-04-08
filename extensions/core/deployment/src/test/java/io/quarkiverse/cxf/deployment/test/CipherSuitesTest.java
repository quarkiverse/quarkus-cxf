package io.quarkiverse.cxf.deployment.test;

import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import jakarta.inject.Inject;
import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

import org.assertj.core.api.Assertions;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

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
public class CipherSuitesTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = configure(new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(HelloService.class, HelloServiceImpl.class))

            .overrideConfigKey("quarkus.http.host", "0.0.0.0")
            /* Server TLS */
            .overrideConfigKey("quarkus.tls.localhost-pkcs12.key-store.p12.path", "localhost-keystore.p12")
            .overrideConfigKey("quarkus.tls.localhost-pkcs12.key-store.p12.password", "secret")
            .overrideConfigKey("quarkus.tls.localhost-pkcs12.cipher-suites", existingCipherSuite(0))
            .overrideConfigKey("quarkus.http.tls-configuration-name", "localhost-pkcs12")
            .overrideConfigKey("quarkus.http.insecure-requests", "disabled")
            /* Service */
            .overrideConfigKey("quarkus.cxf.endpoint.\"/hello\".implementor", HelloServiceImpl.class.getName())
            .overrideConfigKey("quarkus.cxf.endpoint.\"/hello\".logging.enabled", "true")

            /* Named TLS configurations for the clients */
            .overrideConfigKey("quarkus.tls.client-fake-suites.trust-store.p12.path", "target/classes/localhost-truststore.p12")
            .overrideConfigKey("quarkus.tls.client-fake-suites.trust-store.p12.password", "secret")
            .overrideConfigKey("quarkus.tls.client-fake-suites.cipher-suites", existingCipherSuite(1))

            .overrideConfigKey("quarkus.tls.client-pkcs12.trust-store.p12.path",
                    "target/classes/localhost-truststore.p12")
            .overrideConfigKey("quarkus.tls.client-pkcs12.trust-store.p12.password", "secret")
            .overrideConfigKey("quarkus.tls.client-pkcs12.cipher-suites", existingCipherSuite(0))

    );

    private static String existingCipherSuite(int i) {
        try {
            SSLContext sslContext = SSLContext.getDefault();

            // Get the SSLSocketFactory from the SSLContext
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            // Get the supported cipher suites
            String result = sslSocketFactory.getSupportedCipherSuites()[i];
            return result;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    static List<String> clients() {
        return List.of("VertxHttpClient", "URLConnection");
    }

    static QuarkusUnitTest configure(QuarkusUnitTest config) {
        for (String clientName : clients()) {
            config
                    .overrideConfigKey("quarkus.cxf.client." + clientName + "Fake.client-endpoint-url",
                            "https://localhost:8444/services/hello")
                    .overrideConfigKey("quarkus.cxf.client." + clientName + "Fake.logging.enabled", "true")
                    .overrideConfigKey("quarkus.cxf.client." + clientName + "Fake.service-interface",
                            HelloService.class.getName())
                    .overrideConfigKey("quarkus.cxf.client." + clientName + "Fake.http-conduit-factory",
                            clientName + "HTTPConduitFactory")
                    .overrideConfigKey("quarkus.cxf.client." + clientName + "Fake.tls-configuration-name", "client-fake-suites")

                    .overrideConfigKey("quarkus.cxf.client." + clientName + "Existing.client-endpoint-url",
                            "https://localhost:8444/services/hello")
                    .overrideConfigKey("quarkus.cxf.client." + clientName + "Existing.logging.enabled", "true")
                    .overrideConfigKey("quarkus.cxf.client." + clientName + "Existing.service-interface",
                            HelloService.class.getName())
                    .overrideConfigKey("quarkus.cxf.client." + clientName + "Existing.http-conduit-factory",
                            clientName + "HTTPConduitFactory")
                    .overrideConfigKey("quarkus.cxf.client." + clientName + "Existing.tls-configuration-name",
                            "client-pkcs12");
        }
        return config;
    }

    @CXFClient("VertxHttpClientFake")
    HelloService vertxHttpClientFake;

    @CXFClient("URLConnectionFake")
    HelloService urlConnectionFake;

    @CXFClient("VertxHttpClientExisting")
    HelloService vertxHttpClientExisting;

    @CXFClient("URLConnectionExisting")
    HelloService urlConnectionExisting;

    @Inject
    Logger logger;

    @ParameterizedTest
    @MethodSource("clients")
    void ciphersuites(String clientName) {

        HelloService clientFake = client(clientName + "Fake");

        Assertions.assertThatThrownBy(() -> clientFake.hello("Doe")).hasRootCauseMessage(
                "Received fatal alert: handshake_failure");

        HelloService clientExisting = client(clientName + "Existing");
        Assertions.assertThat(clientExisting.hello("Joe")).isEqualTo("Hello Joe");
    }

    HelloService client(String clientName) {
        return switch (clientName) {
            case "VertxHttpClientFake": {
                yield vertxHttpClientFake;
            }
            case "URLConnectionFake": {
                yield urlConnectionFake;
            }
            case "VertxHttpClientExisting": {
                yield vertxHttpClientExisting;
            }
            case "URLConnectionExisting": {
                yield urlConnectionExisting;
            }
            default:
                throw new IllegalArgumentException("Unexpected value: " + clientName);
        };
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
