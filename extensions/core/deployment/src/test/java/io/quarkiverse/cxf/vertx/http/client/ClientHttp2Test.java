package io.quarkiverse.cxf.vertx.http.client;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.HttpServerOptionsCustomizer;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpVersion;
import io.vertx.ext.web.Router;

@Certificates(baseDir = "target/classes", //
        certificates = @Certificate( //
                name = "localhost", //
                password = "secret", //
                formats = { Format.PKCS12 }))
public class ClientHttp2Test {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(HttpVersionService.class))
            .overrideConfigKey("quarkus.tls.key-store.p12.path", "localhost-keystore.p12")
            .overrideConfigKey("quarkus.tls.key-store.p12.password", "secret")
            .overrideConfigKey("quarkus.http.insecure-requests", "disabled")

            .overrideConfigKey("quarkus.tls.soap-clients-tls.trust-store.p12.path", "localhost-truststore.p12")
            .overrideConfigKey("quarkus.tls.soap-clients-tls.trust-store.p12.password", "secret")

            /* Clients */
            .overrideConfigKey("quarkus.cxf.client.hello.client-endpoint-url", "https://localhost:8444/hello")
            .overrideConfigKey("quarkus.cxf.client.hello.service-interface", HttpVersionService.class.getName())
            /* URLConnectionHTTPConduitFactory does not support HTTP/2 */
            .overrideConfigKey("quarkus.cxf.client.hello.http-conduit-factory", "VertxHttpClientHTTPConduitFactory")
            .overrideConfigKey("quarkus.cxf.client.hello.version", "2")
            .overrideConfigKey("quarkus.cxf.client.hello.logging.enabled", "pretty")
            .overrideConfigKey("quarkus.cxf.client.hello.tls-configuration-name", "soap-clients-tls");

    @CXFClient("hello")
    HttpVersionService hello;

    @Test
    void http2() {
        Assertions.assertThat(hello.getHttpVersion()).isEqualTo(HttpVersion.HTTP_2.name());
    }

    void init(@Observes Router router) {
        router.post("/hello").handler(context -> {
            final String v = context.request().version().name();

            context.response()
                    .putHeader("Content-Type", "text/xml; charset=UTF-8")
                    .setStatusCode(200).end(
                            """
                                    <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                                        <soap:Body>
                                            <ns2:getHttpVersionResponse xmlns:ns2="http://test.deployment.cxf.quarkiverse.io/">
                                              <return>%s</return>
                                            </ns2:getHttpVersionResponse>
                                        </soap:Body>
                                    </soap:Envelope>
                                    """
                                    .formatted(v));
        });
    }

    @WebService(serviceName = "HttpVersionService", targetNamespace = "http://test.deployment.cxf.quarkiverse.io/")
    public interface HttpVersionService {

        @WebMethod
        String getHttpVersion();

    }

    @ApplicationScoped
    public static class Http2OnlyServerOptionsCustomizer implements HttpServerOptionsCustomizer {

        @Override
        public void customizeHttpsServer(HttpServerOptions options) {
            // Ensure ALPN is on and only advertise HTTP/2
            options.setUseAlpn(true);
            options.setAlpnVersions(List.of(HttpVersion.HTTP_2));
        }
    }
}
