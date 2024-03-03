package io.quarkiverse.cxf.it.auth.mtls;

import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
public class MutualTlsIT extends MutualTlsTest {

    @Override
    protected String noKeystoreMessage() {
        return "IOException: Error writing to server";
    }
}
