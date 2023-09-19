package io.quarkiverse.cxf.deployment.test;

import java.util.Collections;
import java.util.Set;

import javax.xml.namespace.QName;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import jakarta.xml.soap.SOAPBody;
import jakarta.xml.soap.SOAPEnvelope;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.ws.BindingType;
import jakarta.xml.ws.WebServiceException;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import jakarta.xml.ws.soap.SOAPBinding;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkus.test.QuarkusUnitTest;

public class SoapBindingTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(HelloService.class, HelloServiceImpl.class))
            .overrideConfigKey("quarkus.cxf.endpoint.\"/hello\".implementor", HelloServiceImpl.class.getName())
            .overrideConfigKey("quarkus.cxf.client.hello.client-endpoint-url", "http://localhost:8081/services/hello")
            .overrideConfigKey("quarkus.cxf.client.hello.handlers", SoapBindingChecker.class.getName())
            .overrideConfigKey("quarkus.cxf.client.hello.soap-binding", SOAPBinding.SOAP12HTTP_BINDING)
            .overrideConfigKey("quarkus.cxf.client.hello.service-interface", HelloService.class.getName());

    @CXFClient
    HelloService helloService;

    @Test
    void soapBinding() {
        Assertions.assertThat(helloService.hello("Tom")).isEqualTo("Hello Tom");
    }

    @Test
    void soapBindingOverride() {
        final Client client = ClientProxy.getClient(helloService);
        Assertions.assertThat(client.getEndpoint().getBinding().getBindingInfo().getBindingId())
                .isEqualTo("http://schemas.xmlsoap.org/wsdl/soap12/");
    }

    @WebService
    public interface HelloService {
        @WebMethod
        String hello(String person);
    }

    @WebService(endpointInterface = "io.quarkiverse.cxf.deployment.test.SoapBindingTest$HelloService", serviceName = "HelloService")
    @BindingType(SOAPBinding.SOAP12HTTP_BINDING)
    public static class HelloServiceImpl implements HelloService {

        @Override
        public String hello(String person) {
            return "Hello " + person;
        }
    }

    /**
     * Throws an exception if any handled message does not have the expected NS URI
     * {@code http://www.w3.org/2003/05/soap-envelope}
     */
    public static class SoapBindingChecker implements SOAPHandler<SOAPMessageContext> {

        @Override
        public boolean handleMessage(SOAPMessageContext msgContext) {
            try {
                SOAPEnvelope envelope = msgContext.getMessage().getSOAPPart().getEnvelope();
                SOAPBody body = envelope.getBody();
                Assertions.assertThat(body.getElementQName().getNamespaceURI())
                        .isEqualTo("http://www.w3.org/2003/05/soap-envelope");
            } catch (SOAPException ex) {
                throw new WebServiceException(ex);
            }

            return true;
        }

        @Override
        public boolean handleFault(SOAPMessageContext context) {
            return true;
        }

        @Override
        public void close(MessageContext context) {
        }

        @Override
        public Set<QName> getHeaders() {
            return Collections.emptySet();
        }
    }

}
