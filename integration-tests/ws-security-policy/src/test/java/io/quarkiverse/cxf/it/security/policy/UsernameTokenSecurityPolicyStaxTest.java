package io.quarkiverse.cxf.it.security.policy;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(UsernameTokenSecurityPolicyStaxTest.Profile.class)
public class UsernameTokenSecurityPolicyStaxTest extends AbstractUsernameTokenSecurityPolicyTest {
    public static class Profile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.cxf.endpoint.\"/helloUsernameToken\".security.enable.streaming", "true",
                    "quarkus.cxf.endpoint.\"/helloUsernameTokenAlt\".security.enable.streaming", "true",
                    "quarkus.cxf.endpoint.\"/helloUsernameTokenUncachedNonce\".security.enable.streaming", "true",
                    "quarkus.cxf.client.helloUsernameToken.security.enable.streaming", "true",
                    "quarkus.cxf.client.helloUsernameTokenAlt.security.enable.streaming", "true",
                    "quarkus.cxf.client.helloUsernameTokenNoMustUnderstand.security.enable.streaming", "true",
                    "quarkus.cxf.client.helloNoUsernameToken.security.enable.streaming", "true");
        }

    }

    @Override
    protected String replayAttackDetected() {
        /*
         * The Stax impl does not provide any details, ppalaga checked with CXF 4.0.3 in the code that the exception is
         * thrown due to replay cache match
         */
        return "The security token could not be authenticated or authorized";
    }

    @Override
    protected String noNonce() {
        /*
         * "UsernameToken does not contain a nonce or password is not plain text" is logged
         * but only XML_STREAM_EXC is sent back to the client
         */
        return "XML_STREAM_EXC";
    }

    @Override
    protected int validateTokenFalseCode() {
        return 500;
    }

    @Override
    protected String validateTokenFalseBody() {
        return "The security token could not be authenticated or authorized";
    }

    @Override
    protected String usernameTokenNotSatisfied() {
        return "Assertion {http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702}UsernameToken not satisfied";
    }

}
