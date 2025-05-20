package io.quarkiverse.cxf.wsdl2java.it;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkiverse.cxf.wsdl2java.it.jaxb2.Add;
import io.quarkiverse.cxf.wsdl2java.it.jaxb2.AddList;
import io.quarkiverse.cxf.wsdl2java.it.jaxb2.MyEnumType;
import io.quarkiverse.cxf.wsdl2java.it.jaxb2.Operands;

public class Wsdl2JavaJaxb2Test {

    @Test
    void equalsGenerated() {

        Operands op1 = newOperands(1, 2);
        Operands op2 = newOperands(1, 2);
        Assertions.assertThat(op1.equals(op2)).isTrue();

        Operands op3 = newOperands(2, 2);
        Assertions.assertThat(op1.equals(op3)).isFalse();

    }

    @Test
    void hashcodeGenerated() {
        Operands op = newOperands(0, 0);
        Assertions.assertThat(op.hashCode()).isEqualTo(1874161);
    }

    @Test
    void toStringGenerated() {

        Assertions.assertThat(newOperands(1, 2).toString())
                .matches("io.quarkiverse.cxf.wsdl2java.it.jaxb2.Operands@[^\\[]+\\[a=1, b=2\\]");

    }

    @Test
    void copyable() {

        Operands o = newOperands(3, 5);
        Assertions.assertThat(o.clone()).isNotSameAs(o);
        Assertions.assertThat(o.clone()).isEqualTo(o);

        Assertions.assertThat(o.copyTo(newOperands(0, 0))).isEqualTo(o);

    }

    @Test
    void autoInheritance() {
        Assertions.assertThat(new Add()).isInstanceOf(TestInterface.class);
    }

    @Test
    void mergeable() {

        AddList a = newAddList(3, 5, 6);
        AddList b = newAddList(8, 9, 10);
        AddList result = newAddList();
        result.mergeFrom(a, b);
        Assertions.assertThat(result.getArg0()).containsExactly(3, 5, 6);

        result.mergeFrom(b, a);
        Assertions.assertThat(result.getArg0()).containsExactly(8, 9, 10);

    }

    @Test
    void setters() {
        AddList a = newAddList(1, 2);
        List<Integer> l = Arrays.asList(3, 4);
        a.setArg0(l);
        Assertions.assertThat(a.getArg0()).containsExactly(3, 4);
        Assertions.assertThat(a.getArg0()).isSameAs(l);
    }

    @Test
    void enumValue() {
        Assertions.assertThat(MyEnumType.K_1.enumValue()).isEqualTo("k1");
        Assertions.assertThat(MyEnumType.K_2.enumValue()).isEqualTo("k2");
    }

    @Test
    void jaxbIndex() throws IOException, ClassNotFoundException {
        String packageName = Operands.class.getPackageName();
        ClassLoader cl = Operands.class.getClassLoader();
        String jaxbIndex = packageName.replace('.', '/') + "/jaxb.index";
        System.out.println(jaxbIndex);
        URL url = cl.getResource(jaxbIndex);
        Assertions.assertThat(url).isNotNull();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                cl.loadClass(packageName + "." + line.trim());
            }
        }
    }

    private static Operands newOperands(int a, int b) {
        Operands op = new Operands();
        op.setA(a);
        op.setB(b);
        return op;
    }

    private static AddList newAddList(int... ops) {
        AddList op = new AddList();
        IntStream.of(ops).forEach(i -> op.getArg0().add(Integer.valueOf(i)));
        return op;
    }
}
