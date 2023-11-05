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

    @Override
    Matcher<String> missingSamlErrorMessage(final String endpoint) {
        final String samlMajor;
        final String samlMinor;
        if ("helloSaml1".equals(endpoint)) {
            samlMajor = "1";
            samlMinor = "1";
        } else if ("helloSaml2".equals(endpoint)) {
            samlMajor = "2";
            samlMinor = "0";
        } else {
            throw new IllegalStateException("Unexpected endpoint " + endpoint);
        }

        return Matchers.allOf(
                containsString("These policy alternatives can not be satisfied"),
                /*
                 * https://github.com/rest-assured/rest-assured/issues/1744
                 * The searched substring cannot contain XPath because otherwise RestAssured thinks the matcher is
                 * an XPath matcher and will pass a DOM body instead of String
                 */
                containsString("No SIGNED element found matching one of the XPat"),
                containsString("aths [//saml" + samlMajor + ":Assertion]"),
                containsString("SamlToken: The received token does not match the token inclusion requirement"),
                containsString("{http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702}WssSamlV" + samlMajor + samlMinor
                        + "Token11"));
    }

}
