package io.quarkiverse.cxf.deployment.test;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "fruit"
})
@XmlRootElement(name = "Delete")
public class Delete {

    @XmlElement(name = "fruit")
    protected Fruit fruit;

    /**
     * Obtient la valeur de la propriété addResult.
     *
     */
    public Fruit getFruit() {
        return fruit;
    }

    /**
     * Définit la valeur de la propriété addResult.
     *
     */
    public void setFruit(Fruit value) {
        this.fruit = value;
    }

}
