package io.quarkiverse.cxf.it.security.policy;

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

}
