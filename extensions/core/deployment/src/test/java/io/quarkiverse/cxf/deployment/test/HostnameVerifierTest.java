package io.quarkiverse.cxf.deployment.test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
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
public class HostnameVerifierTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(HelloService.class, HelloServiceImpl.class))

            .overrideConfigKey("quarkus.http.host", "0.0.0.0")
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
            .overrideConfigKey("quarkus.cxf.client.helloVertx.hostname-verifier", "#customHostnameVerifier")
            .overrideConfigKey("quarkus.cxf.client.helloVertx.http-conduit-factory", "VertxHttpClientHTTPConduitFactory")
            .overrideConfigKey("quarkus.cxf.client.helloVertx.tls-configuration-name", "client-pkcs12")

            /* Client with URLConnectionHTTPConduitFactory */
            /*
             * The HTTP client of UrlConnection skips the HostnameVerifier if
             * HostnameChecker.getInstance(HostnameChecker.TYPE_TLS) approves the cert with the host name from the URL.
             * Hence we have to go over the non-localhost name so that our cert containing only localhost does not match
             * and the HostnameVerifier is invoked.
             */
            .overrideConfigKey("quarkus.cxf.client.helloUrlConnection.client-endpoint-url",
                    "https://" + hostname() + ":8444/services/hello")
            .overrideConfigKey("quarkus.cxf.client.helloUrlConnection.logging.enabled", "true")
            .overrideConfigKey("quarkus.cxf.client.helloUrlConnection.service-interface", HelloService.class.getName())
            .overrideConfigKey("quarkus.cxf.client.helloUrlConnection.hostname-verifier", "#customHostnameVerifier")
            .overrideConfigKey("quarkus.cxf.client.helloUrlConnection.http-conduit-factory", "URLConnectionHTTPConduitFactory")
            .overrideConfigKey("quarkus.cxf.client.helloUrlConnection.tls-configuration-name", "client-pkcs12");

    private static String hostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    @CXFClient("helloVertx")
    HelloService helloVertx;

    @CXFClient("helloUrlConnection")
    HelloService helloUrlConnection;

    @Inject
    Logger logger;

    @Inject
    @Named("customHostnameVerifier")
    CustomHostnameVerifier customHostnameVerifier;

    @Test
    void customHostnameVerifierVertx() {
        customHostnameVerifier.getCheckedHostNames().clear();
        Assertions.assertThat(customHostnameVerifier.getCheckedHostNames()).isEmpty();
        customHostnameVerifier.setReturnVal(false);
        Assertions.assertThatThrownBy(() -> helloVertx.hello("Doe")).hasRootCauseMessage(
                "http-conduit-factory = VertxHttpClientHTTPConduitFactory does not support quarkus.cxf.client.helloVertx.hostname-verifier. AllowAllHostnameVerifier can be replaced by using a named TLS configuration (via quarkus.cxf.client.helloVertx.tls-configuration-name) with quarkus.tls.\"tls-bucket-name\".hostname-verification-algorithm set to NONE");
        Assertions.assertThat(customHostnameVerifier.getCheckedHostNames()).isEmpty();
    }

    @Test
    void customHostnameVerifierUrlConnection() {
        final String hostname = hostname();
        customHostnameVerifier.getCheckedHostNames().clear();
        Assertions.assertThat(customHostnameVerifier.getCheckedHostNames()).isEmpty();
        customHostnameVerifier.setReturnVal(false);
        Assertions.assertThatThrownBy(() -> helloUrlConnection.hello("Doe")).hasRootCauseMessage(
                "The https URL hostname does not match the Common Name (CN) on the server certificate in the client's truststore.  Make sure server certificate is correct, or to disable this check (NOT recommended for production) set the CXF client TLS configuration property \"disableCNCheck\" to true.");
        Assertions.assertThat(customHostnameVerifier.getCheckedHostNames()).containsExactly(hostname);
        customHostnameVerifier.setReturnVal(true);
        Assertions.assertThat(helloUrlConnection.hello("Joe")).isEqualTo("Hello Joe");
        Assertions.assertThat(customHostnameVerifier.getCheckedHostNames()).containsExactly(hostname, hostname);
    }

    @ApplicationScoped
    @Named("customHostnameVerifier")
    public static class CustomHostnameVerifier implements HostnameVerifier {

        private boolean returnVal;
        private final List<String> checkedHostNames = new ArrayList<>();

        @Override
        public boolean verify(String hostname, SSLSession session) {
            this.checkedHostNames.add(hostname);
            return returnVal;
        }

        public List<String> getCheckedHostNames() {
            return checkedHostNames;
        }

        public void setReturnVal(boolean returnVal) {
            this.returnVal = returnVal;
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
