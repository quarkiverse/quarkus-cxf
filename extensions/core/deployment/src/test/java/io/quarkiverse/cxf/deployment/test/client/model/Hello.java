
package io.quarkiverse.cxf.deployment.test.client.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for hello complex type
 * </p>
 * .
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * </p>
 *
 * <pre>{@code
 * <complexType name="hello">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="arg0" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "hello", propOrder = {
        "arg0"
})
public class Hello {

    protected String arg0;

    /**
     * Gets the value of the arg0 property.
     *
     * @return
     *         possible object is
     *         {@link String }
     *
     */
    public String getArg0() {
        return arg0;
    }

    /**
     * Sets the value of the arg0 property.
     *
     * @param value
     *        allowed object is
     *        {@link String }
     *
     */
    public void setArg0(String value) {
        this.arg0 = value;
    }

}
