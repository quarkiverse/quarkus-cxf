package io.quarkiverse.cxf.it.http3;

import static io.restassured.RestAssured.get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.http.HttpVersion;

@QuarkusTest
@QuarkusTestResource(Http3ServerResource.class)
public class Http3Test {
    @Test
    void directBufferedAndStreamingRequests() {
        for (int size : new int[] { 1, 20_000, 1 }) {
            get("/http3/direct/" + size).then().statusCode(200).body(is("HTTP_3|true|server.test|" + size));
        }
    }

    @Test
    void discoversHttp3ForSubsequentRequests() {
        get("/http3/discovery/1").then().statusCode(200).body(is("HTTP_2|true|server.test|1"));
        awaitHttp3("discovery");
        get("/http3/discovery/20000").then().statusCode(200).body(is("HTTP_3|true|server.test|20000"));
        get("/http3/discovery/1").then().statusCode(200).body(is("HTTP_3|true|server.test|1"));
    }

    @Test
    void requiresOptInAdvertisementAndEnabledProtocol() {
        for (String client : new String[] { "disabled", "noAdvertisement", "tcpOnly" }) {
            for (int i = 0; i < 2; i++) {
                get("/http3/" + client + "/1").then().statusCode(200).body(is("HTTP_2|true|server.test|1"));
            }
        }
    }

    @Test
    void discoversHttp3FromHttp1Header() {
        get("/http3/discoveryHttp1/1").then().statusCode(200).body(is("HTTP_1_1|false|server.test|1"));
        awaitHttp3("discoveryHttp1");
    }

    @Test
    void rejectsUntrustedCertificateAndWrongHostname() {
        get("/http3/untrusted/1").then().statusCode(500).body(containsString("QUICHE_ERR_TLS_FAIL"));
        get("/http3/wrongHost/1").then().statusCode(500).body(containsString("QUICHE_ERR_TLS_FAIL"));
    }

    @Test
    void mutualTls() {
        get("/http3/mutual/1").then().statusCode(200).body(is("HTTP_3|true|server.test|1"));
        get("/http3/missingClientCertificate/1").then().statusCode(500)
                .body(anyOf(containsString("QUICHE_ERR_TLS_FAIL"), containsString("Connection was closed")));
    }

    @Test
    void rejectsCleartextAndRedirectDowngrade() {
        get("/http3/cleartext/1").then().statusCode(500).body(containsString("HTTP/3 requires an HTTPS endpoint"));
        get("/http3/redirect/1").then().statusCode(500).body(containsString("HTTP/3 requires an HTTPS redirect target"));
    }

    @Test
    void failedSoapRequestIsNotReplayedOverTcp() {
        Http3ServerResource.DROPPED_REQUESTS.clear();
        awaitHttp3("broken");
        get("/http3/drop").then().statusCode(500);
        assertThat(Http3ServerResource.DROPPED_REQUESTS).containsExactly(HttpVersion.HTTP_3);
    }

    private static void awaitHttp3(String client) {
        await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofMillis(100)).untilAsserted(
                () -> get("/http3/" + client + "/1").then().statusCode(200).body(is("HTTP_3|true|server.test|1")));
    }
}
