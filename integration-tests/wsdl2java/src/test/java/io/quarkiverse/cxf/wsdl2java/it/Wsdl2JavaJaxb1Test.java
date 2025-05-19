package io.quarkiverse.cxf.wsdl2java.it;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkiverse.cxf.wsdl2java.it.jaxb1.Operands;
import io.quarkiverse.cxf.wsdl2java.it.jaxb1.Subtract;

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
    void inheritance() {
        // https://github.com/quarkiverse/quarkus-cxf/issues/1816
        // Should be Assertions.assertThat(new Subtract()).isInstanceOf(TestInterface.class);
        Assertions.assertThat(new Subtract()).isNotInstanceOf(TestInterface.class);
    }

    private static Operands newOperands(int a, int b) {
        Operands op = new Operands();
        op.setA(a);
        op.setB(b);
        return op;
    }

}
