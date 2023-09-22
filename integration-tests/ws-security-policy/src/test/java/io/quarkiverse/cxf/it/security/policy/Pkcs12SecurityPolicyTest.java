package io.quarkiverse.cxf.it.security.policy;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(Pkcs12SecurityPolicyTest.Pkcs12Profile.class)
public class Pkcs12SecurityPolicyTest extends AbstractSecurityPolicyTest {

    public static class Pkcs12Profile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.cxf.client.hello.trust-store", "client-truststore.p12",
                    "quarkus.cxf.client.hello.trust-store-type", "PKCS12");
        }

    }

}
