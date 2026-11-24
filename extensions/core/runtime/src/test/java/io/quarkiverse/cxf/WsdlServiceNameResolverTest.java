package io.quarkiverse.cxf;

import java.io.StringReader;

import javax.wsdl.Definition;
import javax.wsdl.WSDLException;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;

public class WsdlServiceNameResolverTest {

    private static final String NS = "http://test.cxf.quarkiverse.io/";
    private static final QName HELLO_PORT_TYPE = new QName(NS, "HelloService");

    @Test
    void singleMatch() throws WSDLException {
        final Definition definition = wsdl("""
                <wsdl:service name="HelloServiceImplService">
                    <wsdl:port name="HelloServiceImplPort" binding="tns:HelloBinding"/>
                </wsdl:service>
                <wsdl:service name="OtherServiceImplService">
                    <wsdl:port name="OtherServiceImplPort" binding="tns:OtherBinding"/>
                </wsdl:service>
                """);
        Assertions.assertThat(resolve(definition, HELLO_PORT_TYPE))
                .isEqualTo(new QName(NS, "HelloServiceImplService"));
    }

    @Test
    void serviceWithMultiplePortsCountedOnce() throws WSDLException {
        final Definition definition = wsdl("""
                <wsdl:service name="HelloServiceImplService">
                    <wsdl:port name="HelloPort1" binding="tns:HelloBinding"/>
                    <wsdl:port name="HelloPort2" binding="tns:HelloBinding12"/>
                </wsdl:service>
                """);
        Assertions.assertThat(resolve(definition, HELLO_PORT_TYPE))
                .isEqualTo(new QName(NS, "HelloServiceImplService"));
    }

    @Test
    void portWithUndefinedBindingIgnored() throws WSDLException {
        final Definition definition = wsdl("""
                <wsdl:service name="BrokenService">
                    <wsdl:port name="BrokenPort" binding="tns:NoSuchBinding"/>
                </wsdl:service>
                <wsdl:service name="HelloServiceImplService">
                    <wsdl:port name="HelloServiceImplPort" binding="tns:HelloBinding"/>
                </wsdl:service>
                """);
        Assertions.assertThat(resolve(definition, HELLO_PORT_TYPE))
                .isEqualTo(new QName(NS, "HelloServiceImplService"));
    }

    @Test
    void noMatch() throws WSDLException {
        final Definition definition = wsdl("""
                <wsdl:service name="OtherServiceImplService">
                    <wsdl:port name="OtherServiceImplPort" binding="tns:OtherBinding"/>
                </wsdl:service>
                """);
        Assertions.assertThatThrownBy(() -> resolve(definition, HELLO_PORT_TYPE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("client myClient")
                .hasMessageContaining("org.acme.HelloService does not specify any serviceName")
                .hasMessageContaining("contains no service having a port of type {" + NS + "}HelloService");
    }

    @Test
    void multipleMatches() throws WSDLException {
        final Definition definition = wsdl("""
                <wsdl:service name="ZuluService">
                    <wsdl:port name="ZuluPort" binding="tns:HelloBinding"/>
                </wsdl:service>
                <wsdl:service name="AlphaService">
                    <wsdl:port name="AlphaPort" binding="tns:HelloBinding12"/>
                </wsdl:service>
                """);
        Assertions.assertThatThrownBy(() -> resolve(definition, HELLO_PORT_TYPE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("contains multiple services having a port of type {" + NS + "}HelloService: [{"
                        + NS + "}AlphaService, {" + NS + "}ZuluService]");
    }

    @Test
    void portTypeNamespaceMustMatch() throws WSDLException {
        final Definition definition = wsdl("""
                <wsdl:service name="HelloServiceImplService">
                    <wsdl:port name="HelloServiceImplPort" binding="tns:HelloBinding"/>
                </wsdl:service>
                """);
        Assertions.assertThat(
                WsdlServiceNameResolver.findServicesByPortType(definition, new QName("http://other/", "HelloService")))
                .isEmpty();
    }

    private static QName resolve(Definition definition, QName portTypeName) {
        return WsdlServiceNameResolver.resolveServiceName(definition, portTypeName, "test.wsdl", "client myClient",
                "org.acme.HelloService");
    }

    /**
     * @param services the {@code wsdl:service} elements to add to a WSDL document defining port types
     *        {@code HelloService} and {@code OtherService} and bindings {@code HelloBinding}, {@code HelloBinding12}
     *        and {@code OtherBinding}
     * @return the parsed WSDL document
     */
    private static Definition wsdl(String services) throws WSDLException {
        final String xml = """
                <wsdl:definitions xmlns:wsdl="http://schemas.xmlsoap.org/wsdl/"
                                  xmlns:tns="%s"
                                  targetNamespace="%s">
                    <wsdl:portType name="HelloService"/>
                    <wsdl:portType name="OtherService"/>
                    <wsdl:binding name="HelloBinding" type="tns:HelloService"/>
                    <wsdl:binding name="HelloBinding12" type="tns:HelloService"/>
                    <wsdl:binding name="OtherBinding" type="tns:OtherService"/>
                    %s
                </wsdl:definitions>
                """.formatted(NS, NS, services);
        final WSDLReader reader = WSDLFactory.newInstance().newWSDLReader();
        reader.setFeature("javax.wsdl.verbose", false);
        return reader.readWSDL(null, new InputSource(new StringReader(xml)));
    }
}
