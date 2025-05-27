package io.quarkiverse.cxf.wsdl2java.it;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkiverse.cxf.wsdl2java.it.simplify.TypeWithElementsProperty;

public class JaxbSimplifyTest {

    @Test
    void simplify() throws NoSuchFieldException, SecurityException {
        /* Make sure the getFoo() and getBar() methods were generated */
        Assertions.assertThat(new TypeWithElementsProperty().getFoo()).isEmpty();
        Assertions.assertThat(new TypeWithElementsProperty().getBar()).isEmpty();
    }
}
