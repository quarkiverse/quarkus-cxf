package io.quarkiverse.cxf.it.annotation.cxfendpoint;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkiverse.cxf.it.HelloService;
import io.quarkiverse.cxf.test.QuarkusCxfClientTestUtil;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class CXFEndpointAnnotationTest {

    @Test
    void annotatedImplementationType() {
        final HelloService client = QuarkusCxfClientTestUtil.getClient(
                "https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test",
                HelloService.class,
                "/soap/path-annotation");
        Assertions.assertThat(client.hello("Joe")).isEqualTo("Hello Joe from PathAnnotationHelloServiceImpl!");
    }

    @Test
    void annotatedImplementationTypeWithBean() {
        final HelloService client = QuarkusCxfClientTestUtil.getClient(
                "https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test",
                HelloService.class,
                "/soap/path-annotation-with-bean");
        Assertions.assertThat(client.hello("Joe")).isEqualTo("Hello Joe from HelloBean!");
    }

}
