package io.quarkiverse.cxf.deployment.test;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Delete", propOrder = {
        "fruit"
})
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
