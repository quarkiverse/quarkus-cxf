
package io.quarkiverse.cxf.it.redirect.retransmitcache;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for retransmitCacheResponse complex type
 * </p>
 * .
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * </p>
 *
 * <pre>{@code
 * <complexType name="retransmitCacheResponse">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="return" type=
"{https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test}retransmitCacheOutput" minOccurs="0"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "retransmitCacheResponse", propOrder = {
        "_return"
})
public class RetransmitCacheResponse {

    @XmlElement(name = "return")
    protected RetransmitCacheOutput _return;

    /**
     * Gets the value of the return property.
     *
     * @return
     *         possible object is
     *         {@link RetransmitCacheOutput }
     *
     */
    public RetransmitCacheOutput getReturn() {
        return _return;
    }

    /**
     * Sets the value of the return property.
     *
     * @param value
     *        allowed object is
     *        {@link RetransmitCacheOutput }
     *
     */
    public void setReturn(RetransmitCacheOutput value) {
        this._return = value;
    }

}
