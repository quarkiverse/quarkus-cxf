package io.quarkiverse.cxf.it.security.policy;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.time.Year;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class TransportPolicyTest {

    @Test
    void hello() {
        /* client calling a service having no policy via HTTPS */
        RestAssured.given()
                .config(PolicyTestUtils.restAssuredConfig())
                .body("Frank")
                .post("/cxf/security-policy/hello")
                .then()
                .statusCode(200)
                .body(is("Hello Frank!"));
    }

    @Test
    void helloAllowAll() {
        /*
         * client calling a service having no policy via https://127.0.0.1
         * Should pass thanks to hostname-verifier = AllowAllHostnameVerifier
         */
        RestAssured.given()
                .config(PolicyTestUtils.restAssuredConfig())
                .body("Frank")
                .post("/cxf/security-policy/helloAllowAll")
                .then()
                .statusCode(200)
                .body(is("Hello Frank!"));
    }

    @Test
    void helloCustomHostnameVerifier() {
        /*
         * client calling a service having no policy via https://127.0.0.1
         * Should pass thanks to hostname-verifier = io.quarkiverse.cxf.it.security.policy.NoopHostnameVerifier
         */
        RestAssured.given()
                .config(PolicyTestUtils.restAssuredConfig())
                .body("Frank")
                .post("/cxf/security-policy/helloCustomHostnameVerifier")
                .then()
                .statusCode(200)
                .body(is("Hello Frank!"));
    }

    @Test
    void helloHttps() {
        /* client calling a service enforcing HTTPS via HTTPS */
        PolicyTestUtils.drainMessages("drainMessages", -1);
        RestAssured.given()
                .config(PolicyTestUtils.restAssuredConfig())
                .body("Frank")
                .post("/cxf/security-policy/helloHttps")
                .then()
                .statusCode(200)
                .body(is("Hello Frank from HTTPS!"));
        final List<String> messages = PolicyTestUtils.drainMessages("drainMessages", 2);
        final String req = messages.get(0);
        Assertions.assertThat(req).contains("<wsse:Security");
        Assertions.assertThat(req).contains("<wsu:Timestamp");
        Assertions.assertThat(req).contains("<wsu:Created");
        Assertions.assertThat(req).contains("<wsu:Expires");

        final String marker = "Payload: ";
        int start = req.indexOf(marker);
        Assertions.assertThat(start).isGreaterThan(0);
        start += marker.length();
        String body = req.substring(start);

        /*
         * A replay should be possible because Timestamps are only cached in conjunction with a message Signature
         * but we do not sign in this scenario
         */
        RestAssured.given()
                .config(PolicyTestUtils.restAssuredConfig())
                .contentType("text/xml")
                .body(body)
                .post("https://localhost:" + getPort() + "/services/helloHttps")
                .then()
                .statusCode(200)
                .body(containsString("Hello Frank from HTTPS!"));

        /* now change the dates to some old date */
        body = body.replaceFirst("<wsu:Created>[^<]*</wsu:Created>", "<wsu:Created>2020-10-01T19:51:36.768Z</wsu:Created>");
        body = body.replaceFirst("<wsu:Expires>[^<]*</wsu:Expires>", "<wsu:Expires>2020-10-01T19:56:36.768Z</wsu:Expires>");
        RestAssured.given()
                .config(PolicyTestUtils.restAssuredConfig())
                .contentType("text/xml")
                .body(body)
                .post("https://localhost:" + getPort()
                        + "/services/helloHttps")
                .then()
                .statusCode(500)
                .body(containsString("Invalid timestamp: The message timestamp has expired"));

        /* now change the dates to some future date */
        int y = Year.now().getValue() + 10;
        body = body.replaceFirst("<wsu:Created>[^<]*</wsu:Created>",
                "<wsu:Created>" + y + "-10-01T19:51:36.768Z</wsu:Created>");
        body = body.replaceFirst("<wsu:Expires>[^<]*</wsu:Expires>",
                "<wsu:Expires>" + y + "-10-01T19:56:36.768Z</wsu:Expires>");
        RestAssured.given()
                .config(PolicyTestUtils.restAssuredConfig())
                .contentType("text/xml")
                .body(body)
                .post("https://localhost:" + getPort()
                        + "/services/helloHttps")
                .then()
                .statusCode(500)
                .body(containsString("Invalid timestamp: The message timestamp is out of range"));

    }

    protected int getPort() {
        final Config config = ConfigProvider.getConfig();
        return config.getValue("quarkus.http.test-ssl-port", Integer.class);
    }

    @Test
    void helloHttpsPkcs12() {
        /* client calling a service enforcing HTTPS via HTTPS */
        RestAssured.given()
                .config(PolicyTestUtils.restAssuredConfig())
                .body("Frank")
                .post("/cxf/security-policy/helloHttpsPkcs12")
                .then()
                .statusCode(200)
                .body(is("Hello Frank from HTTPS!"));
    }

    @Test
    void helloHttp() {
        /* client calling a service enforcing HTTPS via HTTP */
        RestAssured.given()
                .config(PolicyTestUtils.restAssuredConfig())
                .body("Frank")
                .post("/cxf/security-policy/helloHttp")
                .then()
                .statusCode(500)
                .body(containsString("TransportBinding: TLS is not enabled"));
    }

    @Test
    void helloIp() {
        RestAssured.given()
                .config(PolicyTestUtils.restAssuredConfig())
                .body("Frank")
                .post("/cxf/security-policy/helloIp")
                .then()
                /*
                 * expected to fail because the client calls the service via 127.0.0.1 which should not be allowed by
                 * the
                 * default hostname verifier
                 */
                .statusCode(500)
                .body(Matchers.containsString(
                        "The https URL hostname 127.0.0.1 does not match the Common Name (CN) on the server certificate in the client's truststore"));

    }

}
