package io.quarkiverse.cxf.it.security.policy;

import static org.hamcrest.Matchers.containsString;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class UsernameTokenSecurityPolicyTest extends AbstractUsernameTokenSecurityPolicyTest {

    protected String noNonce() {
        return "{http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702}UsernameToken: No Nonce";
    }

    protected String replayAttackDetected() {
        return "A replay attack has been detected";
    }

    protected int validateTokenFalseCode() {
        return 200;
    }

    protected String validateTokenFalseBody() {
        return "Hello Frank from UsernameToken!";
    }

    protected String usernameTokenNotSatisfied() {
        return "These policy alternatives can not be satisfied: \n"
                + "{http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702}SupportingTokens\n"
                + "{http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702}UsernameToken\n"
                + "{http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702}WssUsernameToken11";
    }

    @Test
    @Disabled("https://issues.apache.org/jira/browse/CXF-8940")
    @Override
    void helloUsernameTokenNoMustUnderstand() {
        super.helloUsernameTokenNoMustUnderstand();
    }

    @Override
    Matcher<String> unsignedUnencryptedErrorMessage() {
        return Matchers.allOf(
                containsString("Soap Body is not ENCRYPTED"),
                containsString("Soap Body is not SIGNED"));
    }

}
