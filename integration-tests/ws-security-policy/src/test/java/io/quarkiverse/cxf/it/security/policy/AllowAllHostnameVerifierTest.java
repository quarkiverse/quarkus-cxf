package io.quarkiverse.cxf.it.security.policy;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(AllowAllHostnameVerifierTest.Pkcs12Profile.class)
public class AllowAllHostnameVerifierTest extends AbstractSecurityPolicyTest {

    public static class Pkcs12Profile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.cxf.client.hello.hostname-verifier", "AllowAllHostnameVerifier",
                    // 127.0.0.1 would not work without AllowAllHostnameVerifier
                    "quarkus.cxf.client.hello.client-endpoint-url",
                    "https://127.0.0.1:${quarkus.http.test-ssl-port}/services/hello");
        }

    }

}
