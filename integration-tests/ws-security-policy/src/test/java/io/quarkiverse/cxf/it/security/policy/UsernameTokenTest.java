package io.quarkiverse.cxf.it.security.policy;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class UsernameTokenTest {

    @CXFClient("helloUsernameToken")
    UsernameTokenPolicyHelloService helloUsernameToken;

    @Test
    void helloUsernameToken() {
        Assertions.assertThat(helloUsernameToken.hello("CXF")).isEqualTo("Hello CXF from UsernameToken!");
    }
}
