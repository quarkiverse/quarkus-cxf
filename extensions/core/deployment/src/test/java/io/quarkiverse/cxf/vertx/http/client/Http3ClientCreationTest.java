package io.quarkiverse.cxf.vertx.http.client;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.enterprise.inject.Instance;
import jakarta.jws.WebService;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Clients having {@code 3} in {@code quarkus.cxf.client."client-name".vertx.http-versions} that cannot work are
 * rejected when they are created, rather than on the first request.
 * <p>
 * {@code io.quarkus:quarkus-http3} is not available in this module.
 * The clients that would work with it are tested in {@code integration-tests/http3}.
 */
public class Http3ClientCreationTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = configuration();

    private static QuarkusExtensionTest configuration() {
        final QuarkusExtensionTest result = new QuarkusExtensionTest()
                .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClass(Echo.class))
                .overrideConfigKey("quarkus.proxy.\"corporate\".host", "proxy.example.org")
                .overrideConfigKey("quarkus.proxy.\"corporate\".port", "3128")
                .overrideConfigKey("quarkus.proxy.\"corporate\".non-proxy-hosts", "localhost,*.internal.example.org");
        configure(result, "noQuic", "3", "https://service.example.org/echo");
        configure(result, "noQuicWithFallback", "3,2,1.1", "https://service.example.org/echo");
        configure(result, "plainHttp", "3", "http://service.example.org/echo");
        configure(result, "plainHttpWithFallback", "2,1.1,3", "http://service.example.org/echo");
        configure(result, "plainHttpWsdl", "3", "https://service.example.org/echo");
        result.overrideConfigKey("quarkus.cxf.client.plainHttpWsdl.wsdl", "http://wsdl.example.org/echo?wsdl");
        configure(result, "proxied", "3", "https://service.example.org/echo");
        result.overrideConfigKey("quarkus.cxf.client.proxied.proxy-configuration-name", "corporate");
        configure(result, "proxiedWithFallback", "3,2,1.1", "https://service.example.org/echo");
        result.overrideConfigKey("quarkus.cxf.client.proxiedWithFallback.proxy-configuration-name", "corporate");
        configure(result, "proxiedWsdl", "3", "https://service.internal.example.org/echo");
        result.overrideConfigKey("quarkus.cxf.client.proxiedWsdl.proxy-configuration-name", "corporate")
                .overrideConfigKey("quarkus.cxf.client.proxiedWsdl.wsdl", "https://wsdl.example.org/echo?wsdl");
        configure(result, "nonProxied", "3", "https://service.internal.example.org/echo");
        result.overrideConfigKey("quarkus.cxf.client.nonProxied.proxy-configuration-name", "corporate");
        configure(result, "noProxy", "3", "https://service.example.org/echo");
        result.overrideConfigKey("quarkus.cxf.client.noProxy.proxy-configuration-name", "none");
        return result;
    }

    private static void configure(QuarkusExtensionTest result, String name, String versions, String endpointUrl) {
        final String key = "quarkus.cxf.client." + name;
        result.overrideConfigKey(key + ".service-interface", Echo.class.getName())
                .overrideConfigKey(key + ".client-endpoint-url", endpointUrl)
                .overrideConfigKey(key + ".vertx.http-versions", versions);
    }

    @CXFClient("noQuic")
    Instance<Echo> noQuic;

    @CXFClient("noQuicWithFallback")
    Instance<Echo> noQuicWithFallback;

    @CXFClient("plainHttp")
    Instance<Echo> plainHttp;

    @CXFClient("plainHttpWithFallback")
    Instance<Echo> plainHttpWithFallback;

    @CXFClient("plainHttpWsdl")
    Instance<Echo> plainHttpWsdl;

    @CXFClient("proxied")
    Instance<Echo> proxied;

    @CXFClient("proxiedWithFallback")
    Instance<Echo> proxiedWithFallback;

    @CXFClient("proxiedWsdl")
    Instance<Echo> proxiedWsdl;

    @CXFClient("nonProxied")
    Instance<Echo> nonProxied;

    @CXFClient("noProxy")
    Instance<Echo> noProxy;

    @Test
    void quicNotAvailable() {
        assertThatThrownBy(() -> create(noQuic))
                .hasStackTraceContaining("quarkus.cxf.client.noQuic.vertx.http-versions = 3 requires QUIC")
                .hasStackTraceContaining("add the io.quarkus:quarkus-http3 extension");
        assertThatThrownBy(() -> create(noQuicWithFallback))
                .hasStackTraceContaining("quarkus.cxf.client.noQuicWithFallback.vertx.http-versions = 3,2,1.1 requires QUIC")
                .hasStackTraceContaining("add the io.quarkus:quarkus-http3 extension");
    }

    @Test
    void plainHttp() {
        assertThatThrownBy(() -> create(plainHttp))
                .hasStackTraceContaining("quarkus.cxf.client.plainHttp.vertx.http-versions = 3"
                        + " cannot be used with http://service.example.org, because HTTP/3 requires an https: URL");
        assertThatThrownBy(() -> create(plainHttpWithFallback))
                .hasStackTraceContaining("quarkus.cxf.client.plainHttpWithFallback.vertx.http-versions = 2,1.1,3"
                        + " cannot be used with http://service.example.org, because HTTP/3 requires an https: URL");
        assertThatThrownBy(() -> create(plainHttpWsdl))
                .hasStackTraceContaining("quarkus.cxf.client.plainHttpWsdl.vertx.http-versions = 3"
                        + " cannot be used with http://wsdl.example.org, because HTTP/3 requires an https: URL");
    }

    @Test
    void proxy() {
        assertThatThrownBy(() -> create(proxied))
                .hasStackTraceContaining("quarkus.cxf.client.proxied.vertx.http-versions = 3"
                        + " cannot be used with https://service.example.org, because that destination would be accessed"
                        + " through the proxy proxy.example.org:3128");
        assertThatThrownBy(() -> create(proxiedWithFallback))
                .hasStackTraceContaining("quarkus.cxf.client.proxiedWithFallback.vertx.http-versions = 3,2,1.1"
                        + " cannot be used with https://service.example.org, because that destination would be accessed"
                        + " through the proxy proxy.example.org:3128");
        /* The service is exempted from proxying, but the WSDL is not */
        assertThatThrownBy(() -> create(proxiedWsdl))
                .hasStackTraceContaining("quarkus.cxf.client.proxiedWsdl.vertx.http-versions = 3"
                        + " cannot be used with https://wsdl.example.org, because that destination would be accessed"
                        + " through the proxy proxy.example.org:3128");
    }

    /**
     * These pass the proxy check and thus fail only because QUIC is not available in this module
     */
    @Test
    void nonProxyHosts() {
        assertThatThrownBy(() -> create(nonProxied))
                .hasStackTraceContaining("quarkus.cxf.client.nonProxied.vertx.http-versions = 3 requires QUIC");
        assertThatThrownBy(() -> create(noProxy))
                .hasStackTraceContaining("quarkus.cxf.client.noProxy.vertx.http-versions = 3 requires QUIC");
    }

    private static void create(Instance<Echo> instance) {
        final Echo echo = instance.get();
        instance.destroy(echo);
    }

    @WebService(targetNamespace = "urn:http3-creation")
    public interface Echo {
        String echo(String body);
    }
}
