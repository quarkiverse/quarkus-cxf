package io.quarkiverse.cxf.wsdl2java.it;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkiverse.cxf.wsdl2java.it.parentPointer.Item;

public class JaxbParentPointerTest {

    @Test
    void parentPointer() throws NoSuchFieldException, SecurityException {
        /* Make sure the getParent() method was generated */
        Assertions.assertThat(new Item().getParent()).isNull();
    }
}
