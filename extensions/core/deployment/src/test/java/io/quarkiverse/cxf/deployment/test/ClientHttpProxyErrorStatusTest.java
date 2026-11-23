package io.quarkiverse.cxf.deployment.test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

import org.apache.cxf.transport.http.HTTPException;
import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkus.test.QuarkusExtensionTest;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

/**
 * Makes sure that HTTP statuses {@code >= 400} received by {@code VertxHttpClientHTTPConduit} through an HTTP proxy
 * from a plain {@code http:} service are reported via {@link HTTPException}, see
 * <a href="https://github.com/quarkiverse/quarkus-cxf/issues/2447">#2447</a>.
 * <p>
 * The Quarkus HTTP server on port 8081 acts as the HTTP forward proxy here.
 * It never forwards anything, it just responds itself based on the absolute request URI.
 */
public class ClientHttpProxyErrorStatusTest {

    private static final String PROXY_HOST = "localhost";
    private static final int PROXY_PORT = 8081;
    /* A host different from PROXY_HOST, so that we can tell the request went through the proxy */
    private static final String SERVICE_BASE_URI = "http://127.0.0.1:" + PROXY_PORT;

    /** The request URIs as seen by the proxy */
    private static final List<String> proxiedUris = new CopyOnWriteArrayList<>();

    @RegisterExtension
    public static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(HelloService.class))

            .overrideConfigKey("quarkus.proxy.fwd.host", PROXY_HOST)
            .overrideConfigKey("quarkus.proxy.fwd.port", String.valueOf(PROXY_PORT))

            .overrideConfigKey("quarkus.cxf.client.badGateway.client-endpoint-url", SERVICE_BASE_URI + "/bad-gateway")
            .overrideConfigKey("quarkus.cxf.client.badGateway.service-interface", HelloService.class.getName())
            .overrideConfigKey("quarkus.cxf.client.badGateway.http-conduit-factory", "VertxHttpClientHTTPConduitFactory")
            .overrideConfigKey("quarkus.cxf.client.badGateway.proxy-configuration-name", "fwd")

            .overrideConfigKey("quarkus.cxf.client.redirect.client-endpoint-url", SERVICE_BASE_URI + "/redirect")
            .overrideConfigKey("quarkus.cxf.client.redirect.service-interface", HelloService.class.getName())
            .overrideConfigKey("quarkus.cxf.client.redirect.http-conduit-factory", "VertxHttpClientHTTPConduitFactory")
            .overrideConfigKey("quarkus.cxf.client.redirect.proxy-configuration-name", "fwd")
            .overrideConfigKey("quarkus.cxf.client.redirect.auto-redirect", "true");

    @CXFClient("badGateway")
    // Use Instance to avoid greedy initialization
    Instance<HelloService> badGateway;

    @CXFClient("redirect")
    Instance<HelloService> redirect;

    void init(@Observes Router router) {
        router.route().handler(BodyHandler.create());
        router.post().handler(ctx -> {
            /* An HTTP forward proxy gets the request URI in the absolute form */
            final String uri = ctx.request().uri();
            proxiedUris.add(uri);
            if (uri.endsWith("/bad-gateway")) {
                ctx.response().setStatusCode(502).end();
            } else if (uri.endsWith("/redirect")) {
                ctx.redirect(SERVICE_BASE_URI + "/forbidden");
            } else if (uri.endsWith("/forbidden")) {
                ctx.response().setStatusCode(403).end();
            } else {
                ctx.next();
            }
        });
    }

    @BeforeEach
    void beforeEach() {
        proxiedUris.clear();
    }

    @Test
    void badGateway() {
        Assertions.assertThatThrownBy(() -> badGateway.get().hello("foo"))
                .hasRootCauseInstanceOf(HTTPException.class)
                .hasRootCauseMessage(
                        "HTTP response '502: Bad Gateway' when communicating with " + SERVICE_BASE_URI + "/bad-gateway");
        Assertions.assertThat(proxiedUris).containsExactly(SERVICE_BASE_URI + "/bad-gateway");
    }

    @Test
    void forbiddenAfterRedirect() {
        /* The URI in the exception message must be the one of the last request rather than the initial one */
        Assertions.assertThatThrownBy(() -> redirect.get().hello("foo"))
                .hasRootCauseInstanceOf(HTTPException.class)
                .hasRootCauseMessage(
                        "HTTP response '403: Forbidden' when communicating with " + SERVICE_BASE_URI + "/forbidden");
        Assertions.assertThat(proxiedUris).containsExactly(SERVICE_BASE_URI + "/redirect", SERVICE_BASE_URI + "/forbidden");
    }

    @WebService(serviceName = "HelloService", targetNamespace = "http://test.deployment.cxf.quarkiverse.io/")
    public interface HelloService {

        @WebMethod
        String hello(String person);

    }

}
