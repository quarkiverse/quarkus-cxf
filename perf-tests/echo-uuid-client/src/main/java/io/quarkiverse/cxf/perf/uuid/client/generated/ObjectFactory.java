
package io.quarkiverse.cxf.perf.uuid.client.generated;

import javax.xml.namespace.QName;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import jakarta.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the io.quarkiverse.cxf.perf.uuid.client.generated package. 
 * <p>An ObjectFactory allows you to programmatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private static final QName _EchoUuid_QNAME = new QName("http://l2x6.org/echo-uuid-ws/", "echoUuid");
    private static final QName _EchoUuidResponse_QNAME = new QName("http://l2x6.org/echo-uuid-ws/", "echoUuidResponse");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: io.quarkiverse.cxf.perf.uuid.client.generated
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link EchoUuid }
     * 
     * @return
     *     the new instance of {@link EchoUuid }
     */
    public EchoUuid createEchoUuid() {
        return new EchoUuid();
    }

    /**
     * Create an instance of {@link EchoUuidResponse }
     * 
     * @return
     *     the new instance of {@link EchoUuidResponse }
     */
    public EchoUuidResponse createEchoUuidResponse() {
        return new EchoUuidResponse();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link EchoUuid }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link EchoUuid }{@code >}
     */
    @XmlElementDecl(namespace = "http://l2x6.org/echo-uuid-ws/", name = "echoUuid")
    public JAXBElement<EchoUuid> createEchoUuid(EchoUuid value) {
        return new JAXBElement<>(_EchoUuid_QNAME, EchoUuid.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link EchoUuidResponse }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link EchoUuidResponse }{@code >}
     */
    @XmlElementDecl(namespace = "http://l2x6.org/echo-uuid-ws/", name = "echoUuidResponse")
    public JAXBElement<EchoUuidResponse> createEchoUuidResponse(EchoUuidResponse value) {
        return new JAXBElement<>(_EchoUuidResponse_QNAME, EchoUuidResponse.class, null, value);
    }

}
