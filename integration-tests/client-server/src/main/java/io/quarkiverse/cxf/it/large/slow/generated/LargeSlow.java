
package io.quarkiverse.cxf.it.large.slow.generated;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for largeSlow complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="largeSlow">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="sizeBytes" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         <element name="clientDeserializationDelayMs" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         <element name="serviceExecutionDelayMs" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "largeSlow", propOrder = {
    "sizeBytes",
    "clientDeserializationDelayMs",
    "serviceExecutionDelayMs"
})
public class LargeSlow {

    protected int sizeBytes;
    protected int clientDeserializationDelayMs;
    protected int serviceExecutionDelayMs;

    /**
     * Gets the value of the sizeBytes property.
     * 
     */
    public int getSizeBytes() {
        return sizeBytes;
    }

    /**
     * Sets the value of the sizeBytes property.
     * 
     */
    public void setSizeBytes(int value) {
        this.sizeBytes = value;
    }

    /**
     * Gets the value of the clientDeserializationDelayMs property.
     * 
     */
    public int getClientDeserializationDelayMs() {
        return clientDeserializationDelayMs;
    }

    /**
     * Sets the value of the clientDeserializationDelayMs property.
     * 
     */
    public void setClientDeserializationDelayMs(int value) {
        this.clientDeserializationDelayMs = value;
    }

    /**
     * Gets the value of the serviceExecutionDelayMs property.
     * 
     */
    public int getServiceExecutionDelayMs() {
        return serviceExecutionDelayMs;
    }

    /**
     * Sets the value of the serviceExecutionDelayMs property.
     * 
     */
    public void setServiceExecutionDelayMs(int value) {
        this.serviceExecutionDelayMs = value;
    }

}
