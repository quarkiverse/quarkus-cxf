
package io.quarkiverse.cxf.client.it.rtinit;

import java.util.Objects;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "result", propOrder = {
        "operands",
        "result",
        "even"
})
public class Result {

    /**
     * The defaulValue serves the sole purpose to be able to test {@code wsdl2java}'s {@code -xjc-Xdv} parameter
     * when generating classes from the WSDL of the {@link CalculatorService}
     */
    @XmlElement(defaultValue = "42", namespace = "")
    private int result;

    /**
     * Have some boolean parameter to be able to test {@code wsdl2java}'s {@code -xjc-Xbg} parameter
     * when generating classes from the WSDL of the {@link CalculatorService}
     */
    @XmlElement(namespace = "")
    private boolean even;

    @XmlElement(namespace = "")
    private Operands operands;

    public Result() {
    }

    public Result(int result, Operands operands) {
        super();
        this.result = result;
        this.operands = operands;
    }

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public Operands getOperands() {
        return operands;
    }

    public void setOperands(Operands operands) {
        this.operands = operands;
    }

    public boolean isEven() {
        return even;
    }

    public void setEven(boolean even) {
        this.even = even;
    }

    @Override
    public int hashCode() {
        return Objects.hash(operands, result);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Result other = (Result) obj;
        return Objects.equals(operands, other.operands) && result == other.result;
    }

}
