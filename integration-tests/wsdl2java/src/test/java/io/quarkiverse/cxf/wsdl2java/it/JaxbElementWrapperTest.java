package io.quarkiverse.cxf.wsdl2java.it;

import java.lang.reflect.Field;

import jakarta.xml.bind.annotation.XmlElementWrapper;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkiverse.cxf.wsdl2java.it.elementWrapper.Order;

public class JaxbElementWrapperTest {

    @Test
    void elementWrapper() throws NoSuchFieldException, SecurityException {
        final Field fld = Order.class.getDeclaredField("items");
        final XmlElementWrapper wrapper = fld.getAnnotation(jakarta.xml.bind.annotation.XmlElementWrapper.class);
        Assertions.assertThat(wrapper).isNotNull();
        Assertions.assertThat(wrapper.name()).isEqualTo("items");

    }
}
