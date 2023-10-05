package io.quarkiverse.cxf.it.security.policy;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.restassured.RestAssured;
import io.restassured.config.SSLConfig;

public abstract class AbstractUsernameTokenSecurityPolicyTest {

    @Test
    void helloUsernameToken() {
        String body = helloUsernameTokenCommon("helloUsernameToken");

        // replay should fail because nonces are cached by default
        RestAssured.given()
                .config(RestAssured.config().sslConfig(new SSLConfig().with().trustStore("client-truststore.jks", "password")))
                .contentType("text/xml")
                .body(body)
                .post("/services/helloUsernameToken")
                .then()
                .statusCode(500)
                .body(containsString(replayAttackDetected()));

        // when we remove <wsse:Nonce> the service policy should not allow it
        body = body.replaceFirst("<wsse:Nonce[^<]*</wsse:Nonce>", "");
        RestAssured.given()
                .config(RestAssured.config().sslConfig(new SSLConfig().with().trustStore("client-truststore.jks", "password")))
                .contentType("text/xml")
                .body(body)
                .post("/services/helloUsernameToken")
                .then()
                .statusCode(500)
                .body(containsString(noNonce()));

    }

    protected abstract String noNonce();

    protected abstract String replayAttackDetected();

    @Test
    void helloUsernameTokenAlt() {
        String body = helloUsernameTokenCommon("helloUsernameTokenAlt");

        // replay should succeed because our nonce cache accepts everything
        RestAssured.given()
                .config(RestAssured.config().sslConfig(new SSLConfig().with().trustStore("client-truststore.jks", "password")))
                .contentType("text/xml")
                .body(body)
                .post("/services/helloUsernameTokenAlt")
                .then()
                .statusCode(200)
                .body(containsString("Hello Frank from UsernameToken!"));

        // same with helloUsernameTokenUncachedNonce
        for (int i = 0; i < 2; i++) {
            RestAssured.given()
                    .config(RestAssured.config()
                            .sslConfig(new SSLConfig().with().trustStore("client-truststore.jks", "password")))
                    .contentType("text/xml")
                    .body(body)
                    .post("/services/helloUsernameTokenUncachedNonce")
                    .then()
                    .statusCode(200)
                    .body(containsString("Hello Frank from UsernameToken!"));
        }

        // when we remove <wsse:Nonce> the service policy should not allow it
        final String noNoncebody = body.replaceFirst("<wsse:Nonce[^<]*</wsse:Nonce>", "");
        RestAssured.given()
                .config(RestAssured.config().sslConfig(new SSLConfig().with().trustStore("client-truststore.jks", "password")))
                .contentType("text/xml")
                .body(noNoncebody)
                .post("/services/helloUsernameTokenAlt")
                .then()
                .statusCode(500)
                .body(containsString(noNonce()));

        // the password does not matter because of validate.token = false on the alt endpoint
        body = body.replaceFirst(
                "<wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText\">[^<]*</wsse:Password>",
                "<wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText\">fake</wsse:Password>");
        RestAssured.given()
                .config(RestAssured.config().sslConfig(new SSLConfig().with().trustStore("client-truststore.jks", "password")))
                .contentType("text/xml")
                .body(body)
                .post("/services/helloUsernameTokenAlt")
                .then()
                .statusCode(validateTokenFalseCode())
                .body(containsString(validateTokenFalseBody()));
        // ... but the same should fail on /helloUsernameTokenUncachedNonce where we require correct passwords
        RestAssured.given()
                .config(RestAssured.config()
                        .sslConfig(new SSLConfig().with().trustStore("client-truststore.jks", "password")))
                .contentType("text/xml")
                .body(body)
                .post("/services/helloUsernameTokenUncachedNonce")
                .then()
                .statusCode(500)
                .body(containsString("The security token could not be authenticated or authorized"));

    }

    protected abstract int validateTokenFalseCode();

    protected abstract String validateTokenFalseBody();

    static String helloUsernameTokenCommon(String client) {
        PolicyTestUtils.drainMessages("drainMessages", -1);
        RestAssured.given()
                .config(RestAssured.config().sslConfig(new SSLConfig().with().trustStore("client-truststore.jks", "password")))
                .body("Frank")
                .post("/cxf/security-policy/" + client)
                .then()
                .statusCode(200)
                .body(is("Hello Frank from UsernameToken!"));

        final List<String> messages = PolicyTestUtils.drainMessages("drainMessages", 2);

        final String req = messages.get(0);
        Assertions.assertThat(req).contains(
                "<wsse:Security xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" soap:mustUnderstand=\"1\">");
        Assertions.assertThat(req).contains("<wsse:UsernameToken");
        Assertions.assertThat(req).contains("<wsse:Username>cxf-user</wsse:Username>");
        Assertions.assertThat(req).containsAnyOf(
                "<wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText\">secret</wsse:Password>",
                "<wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText\">fake</wsse:Password>");
        Assertions.assertThat(req).contains("<wsse:Nonce");
        Assertions.assertThat(req).contains("</wsu:Created>\n      </wsse:UsernameToken>");

        final String marker = "Payload: ";
        int start = req.indexOf(marker);
        Assertions.assertThat(start).isGreaterThan(0);
        start += marker.length();
        String body = req.substring(start);
        return body;
    }

    @Test
    void helloNoUsernameToken() {
        PolicyTestUtils.drainMessages("drainMessages", -1);
        /*
         * helloNoUsernameToken has SEI HelloService that has no policy configured but points at /helloUsernameToken
         * endpoint. Hence the requests should go out without UsernameToken and it should fail
         */
        RestAssured.given()
                .config(RestAssured.config().sslConfig(new SSLConfig().with().trustStore("client-truststore.jks", "password")))
                .body("Frank")
                .post("/cxf/security-policy/helloNoUsernameToken")
                .then()
                .statusCode(500)
                .body(containsString(usernameTokenNotSatisfied()));

        final List<String> messages = PolicyTestUtils.drainMessages("drainMessages", 2);
        final String req = messages.get(0);
        Assertions.assertThat(req).doesNotContain("<wsse:UsernameToken");

    }

    protected abstract String usernameTokenNotSatisfied();

}
