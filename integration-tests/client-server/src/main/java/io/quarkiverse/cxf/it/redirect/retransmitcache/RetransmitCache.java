
package io.quarkiverse.cxf.it.redirect.retransmitcache;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for retransmitCache complex type
 * </p>
 * .
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * </p>
 *
 * <pre>{@code
 * <complexType name="retransmitCache">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="arg0" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         <element name="arg1" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "retransmitCache", propOrder = {
        "expectedFileCount",
        "payload"
})
public class RetransmitCache {

    protected int expectedFileCount;
    protected String payload;

    public RetransmitCache() {

    }

    public RetransmitCache(int expectedFileCount, String payload) {
        this.expectedFileCount = expectedFileCount;
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
    public void setPayload(String value) {
        this.payload = value;
    }

    public int getExpectedFileCount() {
        return expectedFileCount;
    }

    public void setExpectedFileCount(int expectedFileCount) {
        this.expectedFileCount = expectedFileCount;
    }

}
