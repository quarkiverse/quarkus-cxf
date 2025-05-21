package io.quarkiverse.cxf.wsdl2java.it;

import java.lang.reflect.Field;

import jakarta.xml.bind.annotation.XmlAnyElement;
import jakarta.xml.bind.annotation.XmlNs;
import jakarta.xml.bind.annotation.XmlSchema;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkiverse.cxf.wsdl2java.it.jaxb3.AcroCase;
import io.quarkiverse.cxf.wsdl2java.it.jaxb3.AnyHolder;
import io.quarkiverse.cxf.wsdl2java.it.jaxb3.BoolHolder;
import io.quarkiverse.cxf.wsdl2java.it.jaxb3.Hello;
import io.quarkiverse.cxf.wsdl2java.it.jaxb3.IntHolder;
import io.quarkiverse.cxf.wsdl2java.it.jaxb3.KebapCase;
import io.quarkiverse.cxf.wsdl2java.it.jaxb3.SnakeCase;

public class Wsdl2JavaJaxb3Test {

    @Test
    void commonsLang() {

        /* equals */
        Hello op1 = newHello("Joe");
        Hello op2 = newHello(new String("Joe".toCharArray()));
        Assertions.assertThat(op1.equals(op2)).isTrue();

        Hello op3 = newHello("Max");
        Assertions.assertThat(op1.equals(op3)).isFalse();

        /* hashCode */
        Assertions.assertThat(op1.hashCode()).isEqualTo(75285);

        Assertions.assertThat(op1.toString())
                .matches("io.quarkiverse.cxf.wsdl2java.it.jaxb3.Hello@[^\\[]+\\[[\\s]*helloName=Joe[\\s]*\\]");
    }

    @Test
    void defaultValue() {
        Assertions.assertThat(new IntHolder().getTheInt()).isEqualTo(2);
    }

    @Test
    void fluentApi() {
        Assertions.assertThat(new Hello().withHelloName("Franz").getHelloName()).isEqualTo("Franz");
    }

    @Test
    void namespacePrefix() {
        final XmlSchema xmlSchemaAnnotation = Hello.class.getPackage()
                .getAnnotation(jakarta.xml.bind.annotation.XmlSchema.class);
        Assertions.assertThat(xmlSchemaAnnotation).isNotNull();
        Assertions.assertThat(xmlSchemaAnnotation.namespace()).isEqualTo("http://test.deployment.cxf.quarkiverse.io/");
        final XmlNs[] xmlns = xmlSchemaAnnotation.xmlns();
        Assertions.assertThat(xmlns).isNotNull().hasSize(1);
        Assertions.assertThat(xmlns[0].prefix()).isEqualTo("jaxb3-test");
        Assertions.assertThat(xmlns[0].namespaceURI()).isEqualTo("http://test.deployment.cxf.quarkiverse.io/");
    }

    @Test
    void valueConstructor() {
        Assertions.assertThat(new Hello("Franz").getHelloName()).isEqualTo("Franz");
    }

    @Test
    void booleanGetter() {
        Assertions.assertThat(new BoolHolder(true).getTheBool()).isTrue();
    }

    @Test
    void camelCase() {
        Assertions.assertThat(new AcroCase()).isNotNull();
        Assertions.assertThat(new SnakeCase()).isNotNull();
        Assertions.assertThat(new KebapCase()).isNotNull();
    }

    @Test
    void inheritance() {
        Assertions.assertThat(new Hello()).isInstanceOf(HelloNameProvider.class);
    }

    @Test
    void wildcard() throws NoSuchFieldException, SecurityException {
        final Field fld = AnyHolder.class.getDeclaredField("content");
        final XmlAnyElement anyElem = fld.getAnnotation(jakarta.xml.bind.annotation.XmlAnyElement.class);
        Assertions.assertThat(anyElem).isNotNull();
        Assertions.assertThat(anyElem.lax()).isTrue();
    }

    private static Hello newHello(String string) {
        Hello result = new Hello();
        result.setHelloName(string);
        return result;
    }
}
