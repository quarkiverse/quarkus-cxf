package io.quarkiverse.cxf.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.enterprise.inject.Instance;
import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.cxf.CXFClientInfo;
import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Clients without {@code quarkus.cxf.client."client-name".client-endpoint-url} are rejected when they are created,
 * rather than sending requests to some default URL.
 */
public class ClientEndpointUrlRequiredTest {

    private static final String SEI = HelloService.class.getName();

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(HelloService.class, UnconfiguredService.class)
                    .addAsResource("wsdllocation/HelloService.wsdl"))
            .overrideConfigKey("quarkus.cxf.client.withUrl.client-endpoint-url", "http://localhost:8081/services/hello")
            .overrideConfigKey("quarkus.cxf.client.withUrl.service-interface", SEI)
            .overrideConfigKey("quarkus.cxf.client.noUrl.service-interface", SEI)
            /* The WSDL has a soap:address, but that one is not accepted as a replacement for client-endpoint-url */
            .overrideConfigKey("quarkus.cxf.client.noUrlWsdl.wsdl", "wsdllocation/HelloService.wsdl")
            .overrideConfigKey("quarkus.cxf.client.noUrlWsdl.service-interface", SEI)
            .overrideConfigKey("quarkus.cxf.client.\"no.url.quoted\".service-interface", SEI);

    @CXFClient("withUrl")
    Instance<HelloService> withUrl;

    @CXFClient("withUrl")
    Instance<CXFClientInfo> withUrlInfo;

    @CXFClient("noUrl")
    Instance<HelloService> noUrl;

    @CXFClient("noUrl")
    Instance<CXFClientInfo> noUrlInfo;

    @CXFClient("noUrlWsdl")
    Instance<HelloService> noUrlWsdl;

    @CXFClient("no.url.quoted")
    Instance<HelloService> noUrlQuoted;

    /** There is no {@code quarkus.cxf.client.notConfigured.*} option at all */
    @CXFClient("notConfigured")
    Instance<HelloService> notConfigured;

    /** There is no client having {@code service-interface} set to {@link UnconfiguredService} */
    @CXFClient
    Instance<UnconfiguredService> unnamed;

    @Test
    void withUrl() {
        final HelloService client = withUrl.get();
        try {
            assertThat(client).isNotNull();
        } finally {
            withUrl.destroy(client);
        }
        assertThat(withUrlInfo.get().getEndpointAddress()).isEqualTo("http://localhost:8081/services/hello");
    }

    @Test
    void noUrl() {
        assertThatThrownBy(() -> create(noUrl))
                .hasStackTraceContaining("Cannot create the client noUrl for the service interface " + SEI
                        + ", because quarkus.cxf.client.noUrl.client-endpoint-url is not set");
        assertThatThrownBy(() -> noUrlInfo.get())
                .hasStackTraceContaining("Cannot create the client noUrl for the service interface " + SEI
                        + ", because quarkus.cxf.client.noUrl.client-endpoint-url is not set");
    }

    @Test
    void noUrlWsdl() {
        assertThatThrownBy(() -> create(noUrlWsdl))
                .hasStackTraceContaining("Cannot create the client noUrlWsdl for the service interface " + SEI
                        + ", because quarkus.cxf.client.noUrlWsdl.client-endpoint-url is not set");
    }

    @Test
    void noUrlQuoted() {
        assertThatThrownBy(() -> create(noUrlQuoted))
                .hasStackTraceContaining("Cannot create the client no.url.quoted for the service interface " + SEI
                        + ", because quarkus.cxf.client.\"no.url.quoted\".client-endpoint-url is not set");
    }

    @Test
    void notConfigured() {
        assertThatThrownBy(() -> create(notConfigured))
                .hasStackTraceContaining("Cannot create the client notConfigured for the service interface " + SEI
                        + ", because quarkus.cxf.client.notConfigured.client-endpoint-url is not set");
    }

    @Test
    void unnamed() {
        assertThatThrownBy(() -> create(unnamed))
                .hasStackTraceContaining("Cannot create a client for the service interface "
                        + UnconfiguredService.class.getName() + " injected into ")
                .hasStackTraceContaining("without a client name, because there is no"
                        + " quarkus.cxf.client.\"client-name\".service-interface = " + UnconfiguredService.class.getName()
                        + " in the configuration; either set quarkus.cxf.client.\"client-name\".service-interface = "
                        + UnconfiguredService.class.getName() + " for exactly one client or inject the client using"
                        + " an explicit client name, e.g. @CXFClient(\"client-name\"); in both cases,"
                        + " quarkus.cxf.client.\"client-name\".client-endpoint-url has to be set for that client");
    }

    private static <T> void create(Instance<T> instance) {
        final T client = instance.get();
        instance.destroy(client);
    }

    @WebService(targetNamespace = "http://test.deployment.cxf.quarkiverse.io/")
    public interface HelloService {
        @WebMethod
        String hello(String name);
    }

    @WebService(targetNamespace = "http://test.deployment.cxf.quarkiverse.io/")
    public interface UnconfiguredService {
        @WebMethod
        String hello(String name);
    }
}
