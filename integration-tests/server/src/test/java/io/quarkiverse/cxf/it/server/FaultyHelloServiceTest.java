package io.quarkiverse.cxf.it.server;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkiverse.cxf.test.QuarkusCxfClientTestUtil;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class FaultyHelloServiceTest {

    @Test
    public void hello() {
        final FaultyHelloService client = QuarkusCxfClientTestUtil.getClient(FaultyHelloService.class, "/soap/faulty-hello");
        Assertions.assertThrows(GreetingException.class, () -> {
            client.faultyHello("Joe");
        });
    }

}
