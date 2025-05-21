package io.quarkiverse.cxf.wsdl2java.it;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkiverse.cxf.wsdl2java.it.jaxb1.Operands;

public class Wsdl2JavaJaxb1Test {

    @Test
    void simpleEquals() {

        Operands op1 = newOperands(1, 2);
        Operands op2 = newOperands(1, 2);
        Assertions.assertThat(op1.equals(op2)).isTrue();

        Operands op3 = newOperands(2, 2);
        Assertions.assertThat(op1.equals(op3)).isFalse();

    }

    @Test
    void simpleHashcode() {
        Operands op = newOperands(0, 0);
        Assertions.assertThat(op.hashCode()).isEqualTo(961);
    }

    @Test
    void injectListenerCode() throws NoSuchFieldException, SecurityException {
        final ArrayList<PropertyChangeEvent> events = new ArrayList<>();
        final Operands op = new Operands();
        op.addVetoableChangeListener("a", events::add);
        op.setA(1);
        // https://github.com/highsource/jaxb-tools/issues/616
        Assertions.assertThat(events)
                .withFailMessage("Set hasSize(1) once https://github.com/highsource/jaxb-tools/issues/616 has been fixed")
                .hasSize(0);
        //Assertions.assertThat(events.get(0).getPropertyName()).isEqualTo("a");
    }

    private static Operands newOperands(int a, int b) {
        Operands op = new Operands();
        op.setA(a);
        op.setB(b);
        return op;
    }

}
