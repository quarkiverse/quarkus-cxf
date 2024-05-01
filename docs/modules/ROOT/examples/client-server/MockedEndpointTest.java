package io.quarkiverse.cxf.it.annotation.cxfendpoint;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkiverse.cxf.annotation.CXFEndpoint;
import io.quarkiverse.cxf.it.HelloService;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class MockedEndpointTest {

    @CXFEndpoint("/helloMock") // <1>
    HelloService helloMockService() {
        final HelloService result = Mockito.mock(HelloService.class);
        Mockito.when(result.hello("Mock")).thenReturn("Hello Mock!");
        return result;
    }

    @CXFClient("helloMock") // <2>
    HelloService helloMockClient;

    @Test
    void helloMock() {
        Assertions.assertThat(helloMockClient.hello("Mock")).isEqualTo("Hello Mock!"); // <3>
    }

}
