package io.quarkiverse.cxf.deployment.test;

import javax.xml.namespace.QName;

import jakarta.enterprise.inject.Instance;
import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.transport.http.HTTPException;
import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * A client whose Service Endpoint Interface (SEI) does not specify any {@code serviceName}.
 * <p>
 * The service name is chosen by the server, so the client cannot derive it reliably from the SEI and has to find it in
 * the WSDL.
 * See https://github.com/quarkiverse/quarkus-cxf/issues/1925
 */
public class ClientWithoutServiceNameTest {

    private static final String NS = "http://test.deployment.cxf.quarkiverse.io/";

    @RegisterExtension
    public static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(HelloService.class, HelloServiceImpl.class)
                    .addAsResource("client-without-service-name/ambiguous.wsdl")
                    .addAsResource("client-without-service-name/no-match.wsdl"))

            /* Service */
            .overrideConfigKey("quarkus.cxf.endpoint.\"/hello\".implementor", HelloServiceImpl.class.getName())

            /* WSDL served by the above service; the only service in it is named HelloServiceImplService */
            .overrideConfigKey("quarkus.cxf.client.liveWsdl.client-endpoint-url", "http://localhost:8081/services/hello")
            .overrideConfigKey("quarkus.cxf.client.liveWsdl.wsdl", "http://localhost:8081/services/hello?wsdl")

            /* No WSDL at all - the service name is not needed */
            .overrideConfigKey("quarkus.cxf.client.noWsdl.client-endpoint-url", "http://localhost:8081/services/hello")

            /* Two services bound to the HelloService port type */
            .overrideConfigKey("quarkus.cxf.client.ambiguous.client-endpoint-url", "http://localhost:8081/services/hello")
            .overrideConfigKey("quarkus.cxf.client.ambiguous.wsdl", "client-without-service-name/ambiguous.wsdl")

            /* No service bound to the HelloService port type */
            .overrideConfigKey("quarkus.cxf.client.noMatch.client-endpoint-url", "http://localhost:8081/services/hello")
            .overrideConfigKey("quarkus.cxf.client.noMatch.wsdl", "client-without-service-name/no-match.wsdl")

            /* The WSDL cannot be fetched - CXF should report it as before */
            .overrideConfigKey("quarkus.cxf.client.wsdl404.client-endpoint-url", "http://localhost:8081/services/hello")
            .overrideConfigKey("quarkus.cxf.client.wsdl404.wsdl", "http://localhost:8081/services/no-such-service?wsdl");

    /* Use Instance to avoid greedy initialization */
    @CXFClient("liveWsdl")
    Instance<HelloService> liveWsdl;

    @CXFClient("noWsdl")
    Instance<HelloService> noWsdl;

    @CXFClient("ambiguous")
    Instance<HelloService> ambiguous;

    @CXFClient("noMatch")
    Instance<HelloService> noMatch;

    @CXFClient("wsdl404")
    Instance<HelloService> wsdl404;

    @Test
    void liveWsdl() {
        final HelloService client = liveWsdl.get();
        Assertions.assertThat(client.hello("foo")).isEqualTo("Hello foo");
        /* The exact name is chosen by the server, we only check that it was taken from the WSDL */
        final QName serviceName = ClientProxy.getClient(client).getEndpoint().getService().getName();
        Assertions.assertThat(serviceName.getNamespaceURI()).isEqualTo(NS);
        Assertions.assertThat(serviceName.getLocalPart()).isNotEmpty();
    }

    @Test
    void noWsdl() {
        Assertions.assertThat(noWsdl.get().hello("bar")).isEqualTo("Hello bar");
    }

    @Test
    void ambiguous() {
        Assertions.assertThatThrownBy(() -> ambiguous.get().hello("foo"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("client ambiguous")
                .hasMessageContaining(HelloService.class.getName() + " does not specify any serviceName")
                .hasMessageContaining("contains multiple services having a port of type {" + NS + "}HelloService: [{"
                        + NS + "}AlphaService, {" + NS + "}BetaService]");
    }

    @Test
    void noMatch() {
        Assertions.assertThatThrownBy(() -> noMatch.get().hello("foo"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("client noMatch")
                .hasMessageContaining("contains no service having a port of type {" + NS + "}HelloService");
    }

    @Test
    void wsdl404() {
        Assertions.assertThatThrownBy(() -> wsdl404.get().hello("foo"))
                .hasRootCauseInstanceOf(HTTPException.class)
                .hasRootCauseMessage(
                        "HTTP response '404: Not Found' when communicating with http://localhost:8081/services/no-such-service?wsdl");
    }

    @WebService
    public interface HelloService {

        @WebMethod
        String hello(String person);

    }

    @WebService
    public static class HelloServiceImpl implements HelloService {

        @Override
        public String hello(String person) {
            return "Hello " + person;
        }
    }

}
