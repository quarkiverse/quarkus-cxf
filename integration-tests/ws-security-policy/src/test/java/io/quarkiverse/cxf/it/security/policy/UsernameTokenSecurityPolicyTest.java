package io.quarkiverse.cxf.it.security.policy;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.config.SSLConfig;

@QuarkusTest
public class UsernameTokenSecurityPolicyTest {

    @Test
    void helloUsernameToken() {
        RestAssured.given()
                .config(RestAssured.config().sslConfig(new SSLConfig().with().trustStore("client-truststore.jks", "password")))
                .body("Frank")
                .post("/cxf/security-policy/helloUsernameToken")
                .then()
                .statusCode(200)
                .body(is("Hello Frank from UsernameToken!"));

        final List<String> messages = new ArrayList<>();

        Awaitility.waitAtMost(30, TimeUnit.SECONDS)
                .until(
                        () -> {
                            final String body = RestAssured.given()
                                    .config(RestAssured.config()
                                            .sslConfig(new SSLConfig().with().trustStore("client-truststore.jks", "password")))
                                    .get("/cxf/security-policy/drainMessages")
                                    .then()
                                    .statusCode(200)
                                    .extract().body().asString();
                            Stream.of(body.split("\\Q|||\\E")).forEach(messages::add);
                            return messages.size() >= 2;
                        });

        String req = messages.get(0);
        Assertions.assertThat(req).contains(
                "<wsse:Security xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\" soap:mustUnderstand=\"1\">");
        Assertions.assertThat(req).contains("<wsse:UsernameToken");
        Assertions.assertThat(req).contains("<wsse:Username>cxf-user</wsse:Username>");
        Assertions.assertThat(req).contains(
                "<wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText\">secret</wsse:Password>");
        Assertions.assertThat(req).contains("<wsse:Nonce");
        Assertions.assertThat(req).contains("<wsu:Created>");

        // quarkus.cxf.client.helloUsernameToken.ws-security.precision-in-milliseconds = false
        final Pattern createdPattern = Pattern.compile("<wsu:Created>([^<]*)</wsu:Created>");
        final Matcher m = createdPattern.matcher(req);
        Assertions.assertThat(m.find()).isTrue();
        Assertions.assertThat(m.group(1).length())
                .isEqualTo(20); // would be 24 with precision-in-milliseconds = true

        // Timestamp action
        Assertions.assertThat(req).contains("</wsu:Expires>\n      </wsu:Timestamp>");

        // quarkus.cxf.endpoint."/helloUsernameToken".ws-security.require-timestamp-expires = true
        final String marker = "Payload: ";
        int start = req.indexOf(marker);
        Assertions.assertThat(start).isGreaterThan(0);
        start += marker.length();
        String body = req.substring(start);

        // remove <wsse:Nonce> to avoid replay attack detection
        body = body.replaceFirst("<wsse:Nonce[^<]*</wsse:Nonce>", "");
        RestAssured.given()
                .config(RestAssured.config().sslConfig(new SSLConfig().with().trustStore("client-truststore.jks", "password")))
                .contentType("text/xml")
                .body(body)
                .post("/services/helloUsernameToken")
                .then()
                .statusCode(200)
                .body(containsString("Hello Frank from UsernameToken!"));

        // set <wsu:Expires> to some old date
        body = body.replaceFirst("<wsu:Expires>[^<]*</wsu:Expires>", "<wsu:Expires>2020-09-28T14:14:20Z</wsu:Expires>");
        // quarkus.cxf.endpoint."/helloUsernameToken".ws-security.timestamp-strict = true - should fail
        RestAssured.given()
                .config(RestAssured.config().sslConfig(new SSLConfig().with().trustStore("client-truststore.jks", "password")))
                .contentType("text/xml")
                .body(body)
                .post("/services/helloUsernameToken")
                .then()
                .statusCode(500)
                .body(containsString("A security error was encountered when verifying the message"));

        // The same request should pass on the Alt endpoint because of
        // quarkus.cxf.endpoint."/helloUsernameTokenAlt".ws-security.timestamp-strict = false
        RestAssured.given()
                .config(RestAssured.config().sslConfig(new SSLConfig().with().trustStore("client-truststore.jks", "password")))
                .contentType("text/xml")
                .body(body)
                .post("/services/helloUsernameTokenAlt")
                .then()
                .statusCode(200)
                .body(containsString("Hello Frank from UsernameToken!"));

        // remove <wsu:Expires>
        body = body.replaceFirst("<wsu:Expires>[^<]*</wsu:Expires>", "");

        // quarkus.cxf.endpoint."/helloUsernameToken".ws-security.require-timestamp-expires = true - should fail
        RestAssured.given()
                .config(RestAssured.config().sslConfig(new SSLConfig().with().trustStore("client-truststore.jks", "password")))
                .contentType("text/xml")
                .body(body)
                .post("/services/helloUsernameToken")
                .then()
                .statusCode(500)
                .body(containsString("A security error was encountered when verifying the message"));

        // The same request should pass on the Alt endpoint because of
        // quarkus.cxf.endpoint."/helloUsernameTokenAlt".ws-security.require-timestamp-expires = false
        RestAssured.given()
                .config(RestAssured.config().sslConfig(new SSLConfig().with().trustStore("client-truststore.jks", "password")))
                .contentType("text/xml")
                .body(body)
                .post("/services/helloUsernameTokenAlt")
                .then()
                .statusCode(200)
                .body(containsString("Hello Frank from UsernameToken!"));

    }

    @Test
    void helloNoUsernameToken() {
        RestAssured.given()
                .config(RestAssured.config().sslConfig(new SSLConfig().with().trustStore("client-truststore.jks", "password")))
                .body("Frank")
                .post("/cxf/security-policy/helloNoUsernameToken")
                .then()
                .statusCode(500)
                .body(containsString("A security error was encountered when verifying the message"));
    }

}
