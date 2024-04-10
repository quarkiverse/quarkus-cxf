package io.quarkiverse.cxf.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkus.test.QuarkusUnitTest;

/**
 * A reproducer for https://github.com/quarkiverse/quarkus-cxf/issues/1326
 */
public class AsmNamingClashTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(root -> root.addClasses(HelloServiceString.class, HelloServiceStringImpl.class))
            .overrideConfigKey("quarkus.cxf.endpoint.\"/helloString\".implementor", HelloServiceStringImpl.class.getName())
            .overrideConfigKey("quarkus.cxf.client.helloString.service-interface", HelloServiceString.class.getName())
            .overrideConfigKey("quarkus.cxf.client.helloString.client-endpoint-url",
                    "http://localhost:8081/services/helloString")
            .overrideConfigKey("quarkus.cxf.endpoint.\"/helloInt\".implementor", HelloServiceIntImpl.class.getName())
            .overrideConfigKey("quarkus.cxf.client.helloInt.service-interface", HelloServiceInt.class.getName())
            .overrideConfigKey("quarkus.cxf.client.helloInt.client-endpoint-url", "http://localhost:8081/services/helloInt")
            .assertException(t -> Assertions.assertThat(t).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(
                            "Cannot overwrite an existing generated class file io/quarkiverse/cxf/deployment/test/jaxws_asm/Hello with a different content"));

    @CXFClient("helloString")
    HelloServiceString helloString;

    @CXFClient("helloInt")
    HelloServiceInt helloInt;

    @Test
    void payloadLogged() throws IOException {

        Assertions.assertThat(helloString.hello("Joe")).isEqualTo("Hello Joe!");
        Assertions.assertThat(helloInt.hello(42)).isEqualTo("Hello 42!");

    }

    @WebService(targetNamespace = "http://deployment.logging.features.cxf.quarkiverse.io/")
    public interface HelloServiceString {

        @WebMethod
        String hello(String text);

    }

    @WebService(targetNamespace = "http://deployment.logging.features.cxf.quarkiverse.io/")
    public interface HelloServiceInt {

        @WebMethod
        String hello(int i);

    }

    @WebService(serviceName = "HelloService")
    public static class HelloServiceStringImpl implements HelloServiceString {

        @WebMethod
        @Override
        public String hello(String text) {
            return "Hello " + text + "!";
        }

    }

    @WebService(serviceName = "HelloService")
    public static class HelloServiceIntImpl implements HelloServiceInt {

        @WebMethod
        @Override
        public String hello(int text) {
            return "Hello " + text + "!";
        }

    }
}
