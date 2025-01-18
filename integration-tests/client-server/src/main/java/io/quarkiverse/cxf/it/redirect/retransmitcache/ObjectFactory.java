
package io.quarkiverse.cxf.it.redirect.retransmitcache;

import javax.xml.namespace.QName;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import jakarta.xml.bind.annotation.XmlRegistry;

/**
 * This object contains factory methods for each
 * Java content interface and Java element interface
 * generated in the io.quarkiverse.cxf.it.redirect.retransmitcache package.
 * <p>
 * An ObjectFactory allows you to programmatically
 * construct new instances of the Java representation
 * for XML content. The Java representation of XML
 * content can consist of schema derived interfaces
 * and classes representing the binding of schema
 * type definitions, element declarations and model
 * groups. Factory methods for each of these are
 * provided in this class.
 *
 */
@XmlRegistry
public class ObjectFactory {

    private static final QName _RetransmitCache_QNAME = new QName(
            "https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test", "retransmitCache");
    private static final QName _RetransmitCacheResponse_QNAME = new QName(
            "https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test", "retransmitCacheResponse");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package:
     * io.quarkiverse.cxf.it.redirect.retransmitcache
     *
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link RetransmitCache }
     *
     * @return
     *         the new instance of {@link RetransmitCache }
     */
    public RetransmitCache createRetransmitCache() {
        return new RetransmitCache();
    }

    /**
     * Create an instance of {@link RetransmitCacheResponse }
     *
     * @return
     *         the new instance of {@link RetransmitCacheResponse }
     */
    public RetransmitCacheResponse createRetransmitCacheResponse() {
        return new RetransmitCacheResponse();
    }

    /**
     * Create an instance of {@link RetransmitCacheOutput }
     *
     * @return
     *         the new instance of {@link RetransmitCacheOutput }
     */
    public RetransmitCacheOutput createRetransmitCacheOutput() {
        return new RetransmitCacheOutput();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RetransmitCache }{@code >}
     *
     * @param value
     *        Java instance representing xml element's value.
     * @return
     *         the new instance of {@link JAXBElement }{@code <}{@link RetransmitCache }{@code >}
     */
    @XmlElementDecl(namespace = "https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test", name = "retransmitCache")
    public JAXBElement<RetransmitCache> createRetransmitCache(RetransmitCache value) {
        return new JAXBElement<>(_RetransmitCache_QNAME, RetransmitCache.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RetransmitCacheResponse }{@code >}
     *
     * @param value
     *        Java instance representing xml element's value.
     * @return
     *         the new instance of {@link JAXBElement }{@code <}{@link RetransmitCacheResponse }{@code >}
     */
    @XmlElementDecl(namespace = "https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test", name = "retransmitCacheResponse")
    public JAXBElement<RetransmitCacheResponse> createRetransmitCacheResponse(RetransmitCacheResponse value) {
        return new JAXBElement<>(_RetransmitCacheResponse_QNAME, RetransmitCacheResponse.class, null, value);
    }

}
