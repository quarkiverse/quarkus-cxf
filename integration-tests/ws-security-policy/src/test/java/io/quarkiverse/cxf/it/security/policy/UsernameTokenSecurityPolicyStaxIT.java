package io.quarkiverse.cxf.it.security.policy;

import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
public class UsernameTokenSecurityPolicyStaxIT extends UsernameTokenSecurityPolicyStaxTest {

    @Override
    protected int getPort() {
        // final Config config = ConfigProvider.getConfig();
        // does not seem to work return config.getValue("quarkus.http.test-ssl-port", Integer.class);
        return 8444;
    }

}
