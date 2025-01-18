
package io.quarkiverse.cxf.it.redirect.retransmitcache;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for retransmitCacheOutput complex type
 * </p>
 * .
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * </p>
 *
 * <pre>{@code
 * <complexType name="retransmitCacheOutput">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="delayMs" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         <element name="payload" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlType(name = "retransmitCacheOutput", propOrder = {
        "payload"
})
public class RetransmitCacheOutput {

    protected String payload;

    public RetransmitCacheOutput() {

    }

    public RetransmitCacheOutput(String payload) {
        this.payload = payload;
    }

    /**
     * Gets the value of the payload property.
     *
     * @return
     *         possible object is
     *         {@link String }
     *
     */
    public String getPayload() {
        return payload;
    }

    /**
     * Sets the value of the payload property.
     *
     * @param value
     *        allowed object is
     *        {@link String }
     *
     */
    @XmlElement(name = "payload")
    public void setPayload(String value) {
        this.payload = value;
    }

}
