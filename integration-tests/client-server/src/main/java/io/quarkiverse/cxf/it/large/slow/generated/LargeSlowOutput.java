
package io.quarkiverse.cxf.it.large.slow.generated;

import io.quarkus.logging.Log;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for largeSlowOutput complex type</p>.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 *
 * <pre>{@code
 * <complexType name="largeSlowOutput">
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
@XmlType(name = "largeSlowOutput", propOrder = {
    "delayMs",
    "payload"
})
public class LargeSlowOutput {

    protected int delayMs;
    protected String payload;

    public LargeSlowOutput() {

    }
    public LargeSlowOutput(int delayMs, String payload) {
        this.delayMs = delayMs;
        this.payload = payload;
    }

    /**
     * Gets the value of the delayMs property.
     *
     */
    public int getDelayMs() {
        return delayMs;
    }

    /**
     * Sets the value of the delayMs property.
     *
     */
    @XmlElement(name = "delayMs")
    public void setDelayMs(int delayMs) {
        Log.infof("Sleeping for %d ms", delayMs);
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        this.delayMs = delayMs;
    }

    /**
     * Gets the value of the payload property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getPayload() {
        return payload;
    }

    /**
     * Sets the value of the payload property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    @XmlElement(name = "payload")
    public void setPayload(String value) {
        this.payload = value;
    }

}
