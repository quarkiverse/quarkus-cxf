package io.quarkiverse.cxf.it.security.policy;

import static org.hamcrest.Matchers.containsString;

import java.util.LinkedHashMap;
import java.util.Map;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(UsernameTokenSecurityPolicyStaxTest.Profile.class)
public class UsernameTokenSecurityPolicyStaxTest extends AbstractUsernameTokenSecurityPolicyTest {
    public static class Profile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            final Map<String, String> map = new LinkedHashMap<>();
            map.put("quarkus.cxf.endpoint.\"/helloUsernameToken\".security.enable.streaming", "true");
            map.put("quarkus.cxf.endpoint.\"/helloUsernameTokenAlt\".security.enable.streaming", "true");
            map.put("quarkus.cxf.endpoint.\"/helloUsernameTokenUncachedNonce\".security.enable.streaming", "true");
            map.put("quarkus.cxf.endpoint.\"/helloEncryptSign\".security.enable.streaming", "true");
            map.put("quarkus.cxf.endpoint.\"/helloEncryptSignCrypto\".security.enable.streaming", "true");
            map.put("quarkus.cxf.endpoint.\"/helloSaml1\".security.enable.streaming", "true");
            map.put("quarkus.cxf.endpoint.\"/helloSaml2\".security.enable.streaming", "true");
            map.put("quarkus.cxf.client.helloUsernameToken.security.enable.streaming", "true");
            map.put("quarkus.cxf.client.helloUsernameTokenAlt.security.enable.streaming", "true");
            map.put("quarkus.cxf.client.helloUsernameTokenNoMustUnderstand.security.enable.streaming", "true");
            map.put("quarkus.cxf.client.helloNoUsernameToken.security.enable.streaming", "true");
            map.put("quarkus.cxf.client.helloEncryptSign.security.enable.streaming", "true");
            map.put("quarkus.cxf.client.helloEncryptSignCrypto.security.enable.streaming", "true");
            map.put("quarkus.cxf.client.helloSaml1.security.enable.streaming", "true");
            map.put("quarkus.cxf.client.helloSaml2.security.enable.streaming", "true");
            return map;
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

    @Override
    Matcher<String> unsignedUnencryptedErrorMessage() {
        /* The Stax implmentation does not honor security.return.security.error = true */
        return containsString("<faultstring>XML_STREAM_EXC</faultstring>");
    }

    @Override
    Matcher<String> missingSamlErrorMessage(final String endpoint) {
        /* The Stax implmentation does not honor security.return.security.error = true */
        return containsString("An error was discovered processing the &lt;wsse:Security> header");
    }

    @Disabled("https://github.com/quarkiverse/quarkus-cxf/issues/1095")
    @Override
    @Test
    void helloSaml1() {
        super.helloSaml1();
    }

    @Disabled("https://github.com/quarkiverse/quarkus-cxf/issues/1095")
    @Override
    @Test
    void helloSaml2() {
        super.helloSaml1();
    }

}
