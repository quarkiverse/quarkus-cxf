
package io.quarkiverse.cxf.it.large.slow.generated;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for largeSlowResponse complex type</p>.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.</p>
 * 
 * <pre>{@code
 * <complexType name="largeSlowResponse">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="return" type="{https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test}largeSlowOutput" minOccurs="0"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "largeSlowResponse", propOrder = {
    "_return"
})
public class LargeSlowResponse {

    @XmlElement(name = "return")
    protected LargeSlowOutput _return;

    /**
     * Gets the value of the return property.
     * 
     * @return
     *     possible object is
     *     {@link LargeSlowOutput }
     *     
     */
    public LargeSlowOutput getReturn() {
        return _return;
    }

    /**
     * Sets the value of the return property.
     * 
     * @param value
     *     allowed object is
     *     {@link LargeSlowOutput }
     *     
     */
    public void setReturn(LargeSlowOutput value) {
        this._return = value;
    }

}
