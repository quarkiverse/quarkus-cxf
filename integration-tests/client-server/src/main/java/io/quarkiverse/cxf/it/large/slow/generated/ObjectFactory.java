
package io.quarkiverse.cxf.it.large.slow.generated;

import javax.xml.namespace.QName;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import jakarta.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the io.quarkiverse.cxf.it.large.slow.generated package. 
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

    private static final QName _LargeSlow_QNAME = new QName("https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test", "largeSlow");
    private static final QName _LargeSlowResponse_QNAME = new QName("https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test", "largeSlowResponse");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: io.quarkiverse.cxf.it.large.slow.generated
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link LargeSlow }
     * 
     * @return
     *     the new instance of {@link LargeSlow }
     */
    public LargeSlow createLargeSlow() {
        return new LargeSlow();
    }

    /**
     * Create an instance of {@link LargeSlowResponse }
     * 
     * @return
     *     the new instance of {@link LargeSlowResponse }
     */
    public LargeSlowResponse createLargeSlowResponse() {
        return new LargeSlowResponse();
    }

    /**
     * Create an instance of {@link LargeSlowOutput }
     * 
     * @return
     *     the new instance of {@link LargeSlowOutput }
     */
    public LargeSlowOutput createLargeSlowOutput() {
        return new LargeSlowOutput();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link LargeSlow }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link LargeSlow }{@code >}
     */
    @XmlElementDecl(namespace = "https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test", name = "largeSlow")
    public JAXBElement<LargeSlow> createLargeSlow(LargeSlow value) {
        return new JAXBElement<>(_LargeSlow_QNAME, LargeSlow.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link LargeSlowResponse }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link LargeSlowResponse }{@code >}
     */
    @XmlElementDecl(namespace = "https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test", name = "largeSlowResponse")
    public JAXBElement<LargeSlowResponse> createLargeSlowResponse(LargeSlowResponse value) {
        return new JAXBElement<>(_LargeSlowResponse_QNAME, LargeSlowResponse.class, null, value);
    }

}
