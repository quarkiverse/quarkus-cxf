package io.quarkiverse.cxf.it.security.policy;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;

public abstract class AbstractUsernameTokenSecurityPolicyTest extends AbstractFipsAwareTest {

    @Test
    void helloUsernameToken() {
        String body = helloUsernameTokenCommon("helloUsernameToken");

        // replay should fail because nonces are cached by default
        RestAssured.given()
                .config(PolicyTestUtils.restAssuredConfig())
                .contentType("text/xml")
                .body(body)
                .post("/services/helloUsernameToken")
                .then()
                .statusCode(500)
                .body(containsString(replayAttackDetected()));

        // when we remove <wsse:Nonce> the service policy should not allow it
        body = body.replaceFirst("<wsse:Nonce[^<]*</wsse:Nonce>", "");
        RestAssured.given()
                .config(PolicyTestUtils.restAssuredConfig())
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
                .config(PolicyTestUtils.restAssuredConfig())
                .contentType("text/xml")
                .body(body)
                .post("/services/helloUsernameTokenAlt")
                .then()
                .statusCode(200)
                .body(containsString("Hello Frank from UsernameToken!"));

        // same with helloUsernameTokenUncachedNonce
        for (int i = 0; i < 2; i++) {
            RestAssured.given()
                    .config(PolicyTestUtils.restAssuredConfig())
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
                .config(PolicyTestUtils.restAssuredConfig())
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
                .config(PolicyTestUtils.restAssuredConfig())
                .contentType("text/xml")
                .body(body)
                .post("/services/helloUsernameTokenAlt")
                .then()
                .statusCode(validateTokenFalseCode())
                .body(containsString(validateTokenFalseBody()));
        // ... but the same should fail on /helloUsernameTokenUncachedNonce where we require correct passwords
        RestAssured.given()
                .config(PolicyTestUtils.restAssuredConfig())
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
                .config(PolicyTestUtils.restAssuredConfig())
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

        return extractPayload(req);
    }

    static String extractPayload(final String req) {
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
                .config(PolicyTestUtils.restAssuredConfig())
                .body("Frank")
                .post("/cxf/security-policy/helloNoUsernameToken")
                .then()
                .statusCode(500)
                .body(containsString(usernameTokenNotSatisfied()));

        final List<String> messages = PolicyTestUtils.drainMessages("drainMessages", 2);
        final String req = messages.get(0);
        Assertions.assertThat(req).doesNotContain("<wsse:UsernameToken");

    }

    @Test
    void helloUsernameTokenNoMustUnderstand() {
        PolicyTestUtils.drainMessages("drainMessages", -1);
        RestAssured.given()
                .config(PolicyTestUtils.restAssuredConfig())
                .body("helloUsernameTokenNoMustUnderstand")
                .post("/cxf/security-policy/helloUsernameTokenNoMustUnderstand")
                .then()
                .statusCode(200)
                .body(is("Hello helloUsernameTokenNoMustUnderstand from UsernameToken!"));

        final List<String> messages = PolicyTestUtils.drainMessages("drainMessages", 2);

        final String req = messages.get(0);
        Assertions.assertThat(req).doesNotContain("soap:mustUnderstand=\"1\"");

    }

    protected abstract String usernameTokenNotSatisfied();

    @Test
    void helloEncryptSign() throws IOException {
        encryptSign("helloEncryptSign");
    }

    @Test
    void helloEncryptSignCrypto() throws IOException {
        encryptSign("helloEncryptSignCrypto");
    }

    void encryptSign(String endpoint) throws IOException {
        failFipsInNative();

        PolicyTestUtils.drainMessages("drainMessages", -1);

        final String requestPayload = "random person";
        final String responsePayload = "Hello random person from EncryptSign!";
        ValidatableResponse response = RestAssured.given()
                .config(PolicyTestUtils.restAssuredConfig())
                .body(requestPayload)
                .post("/cxf/security-policy/" + endpoint)
                .then();

        if (isFipsEnabled()) {
            response.statusCode(500)
                    .body(containsString("java.security.NoSuchAlgorithmException: Cannot find any provider supporting"));

            final List<String> messages = PolicyTestUtils.drainMessages("drainMessages", 2);
            //no further testing is required
            PolicyTestUtils.drainMessages("drainMessages", 2);
            return;
        }

        //non-fips environment
        response.statusCode(200)
                .body(is(responsePayload));

        final List<String> messages = PolicyTestUtils.drainMessages("drainMessages", 2);

        final String req = messages.get(0);
        Assertions.assertThat(req).doesNotContain(requestPayload);
        Assertions.assertThat(req).contains("<xenc:EncryptedKey ");
        Assertions.assertThat(req).contains(":Signature ");
        Assertions.assertThat(req).contains(":SignatureValue>");
        Assertions.assertThat(req).contains("<xenc:EncryptedData ");

        final String resp = messages.get(1);
        Assertions.assertThat(resp).doesNotContain(responsePayload);
        Assertions.assertThat(resp).contains("<xenc:EncryptedKey ");
        Assertions.assertThat(resp).contains(":Signature ");
        Assertions.assertThat(resp).contains(":SignatureValue>");
        Assertions.assertThat(resp).contains("<xenc:EncryptedData ");

        /* Unsigned and unencrypted message must fail */
        final String unsignedUnencrypted = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                + "  <soap:Body>\n"
                + "    <ns2:hello xmlns:ns2=\"http://policy.security.it.cxf.quarkiverse.io/\">\n"
                + "      <arg0>helloEncryptSign</arg0>\n"
                + "    </ns2:hello>\n"
                + "  </soap:Body>\n"
                + "</soap:Envelope>";
        RestAssured.given()
                .config(PolicyTestUtils.restAssuredConfig())
                .contentType("text/xml")
                .body(unsignedUnencrypted)
                .post("/services/" + endpoint)
                .then()
                .statusCode(500)
                .body(unsignedUnencryptedErrorMessage());

    }

    abstract Matcher<String> unsignedUnencryptedErrorMessage();

    @Test
    void helloSaml1() {
        saml("helloSaml1");
    }

    @Test
    void helloSaml2() {
        saml("helloSaml2");
    }

    void saml(String endpoint) {
        PolicyTestUtils.drainMessages("drainMessages", -1);

        final String requestPayload = "random saml person";
        final String responsePayload = "Hello random saml person from " + endpoint + "!";
        RestAssured.given()
                .config(PolicyTestUtils.restAssuredConfig())
                .body(requestPayload)
                .post("/cxf/security-policy/" + endpoint)
                .then()
                .statusCode(200)
                .body(is(responsePayload));

        final List<String> messages = PolicyTestUtils.drainMessages("drainMessages", 2);

        final String req = messages.get(0);
        if ("helloSaml1".equals(endpoint)) {
            Assertions.assertThat(req).contains("\"urn:oasis:names:tc:SAML:1.0:assertion\"");
            Assertions.assertThat(req).contains("NameIdentifier Format=\"urn:oasis:names:tc:SAML:1.1");
        } else if ("helloSaml2".equals(endpoint)) {
            Assertions.assertThat(req).contains("\"urn:oasis:names:tc:SAML:2.0:assertion\"");
            Assertions.assertThat(req).contains("NameFormat=\"urn:oasis:names:tc:SAML:2.0");
        } else {
            throw new IllegalStateException("Unexpected endpoint " + endpoint);
        }
        Assertions.assertThat(req).contains(":Signature ");
        Assertions.assertThat(req).contains(":SignatureValue>");

        final String resp = messages.get(1);
        Assertions.assertThat(resp).contains(":Signature ");
        Assertions.assertThat(resp).contains(":SignatureValue>");

        /* Unsigned and unencrypted message must fail */
        final String unsignedUnencrypted = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                + "  <soap:Body>\n"
                + "    <ns2:hello xmlns:ns2=\"http://policy.security.it.cxf.quarkiverse.io/\">\n"
                + "      <arg0>" + requestPayload + "</arg0>\n"
                + "    </ns2:hello>\n"
                + "  </soap:Body>\n"
                + "</soap:Envelope>";
        RestAssured.given()
                .config(PolicyTestUtils.restAssuredConfig())
                .contentType("text/xml")
                .body(unsignedUnencrypted)
                .post("https://localhost:" + getPort() + "/services/" + endpoint)
                .then()
                .statusCode(500)
                .body(missingSamlErrorMessage(endpoint));

    }

    abstract Matcher<String> missingSamlErrorMessage(String endpoint);

    protected int getPort() {
        final Config config = ConfigProvider.getConfig();
        return config.getValue("quarkus.http.test-ssl-port", Integer.class);
    }

}
