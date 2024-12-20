package io.quarkiverse.cxf.it.redirect;

import java.util.concurrent.ExecutionException;

import org.assertj.core.api.Assumptions;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkiverse.cxf.HTTPConduitImpl;
import io.quarkiverse.cxf.it.large.slow.LargeSlowServiceImpl;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;

@QuarkusTest
class RedirectTest {

    private static final Logger log = Logger.getLogger(RedirectTest.class);

    @BeforeEach
    void beforeEach() {
        RestAssured.given()
                .delete("/RedirectRest/selfRedirect")
                .then()
                .statusCode(204);
    }

    @ParameterizedTest
    @ValueSource(strings = { //
            "sync/singleRedirect", //
            "async/singleRedirect", //

            "sync/doubleRedirect", //
            "async/doubleRedirect", //

            "sync/tripleRedirect", //
            "async/tripleRedirect", //

            "sync/doubleRedirectMaxRetransmits2", //
            "async/doubleRedirectMaxRetransmits2", //

            "sync/doubleRedirectMaxRetransmits2MaxSameUri0", //
            "async/doubleRedirectMaxRetransmits2MaxSameUri0", //

            "sync/maxSameUri1", //
            "async/maxSameUri1", //

            "sync/maxSameUri3", //
            "async/maxSameUri3" //

    })
    void redirect(String endpoint) throws InterruptedException, ExecutionException {
        log.infof("Testing endpoint %s", endpoint);

        try {
            final int sizeBytes = 16; // smallish, suits single buffer in
            // VertxHttpClientHTTPConduit.RequestBodyHandler.bodyRecorder
            getResponse(endpoint, sizeBytes)
                    .statusCode(200)
                    .body(Matchers.is(LargeSlowServiceImpl.largeString(sizeBytes)));
        } finally {
            RestAssured.given()
                    .delete("/RedirectRest/selfRedirect")
                    .then()
                    .statusCode(204);
        }
        try {
            final int sizeBytes = 9 * 1024; // biggish, forces multiple buffers in
            // VertxHttpClientHTTPConduit.RequestBodyHandler.bodyRecorder
            getResponse(endpoint, sizeBytes)
                    .statusCode(200)
                    .body(Matchers.is(LargeSlowServiceImpl.largeString(sizeBytes)));
        } finally {
            RestAssured.given()
                    .delete("/RedirectRest/selfRedirect")
                    .then()
                    .statusCode(204);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { //
            "sync/noAutoRedirect", //
            "async/noAutoRedirect" //
    })
    void noAutoRedirect(String endpoint) {
        int sizeBytes = 16;
        getResponse(endpoint, sizeBytes)
                .statusCode(500)
                .body(
                        CoreMatchers.either(
                                Matchers.matchesPattern(
                                        "\\QReceived redirection status 307 from http://localhost:\\E[0-9]+\\Q/RedirectRest/singleRedirect"
                                                + " by client noAutoRedirect but following redirects is not enabled for this client."
                                                + " You may want to set quarkus.cxf.client.noAutoRedirect.auto-redirect = true\\E"))
                                .or(CoreMatchers.containsString("Unexpected EOF in prolog")));
    }

    @ParameterizedTest
    @ValueSource(strings = { //
            "sync/doubleRedirectMaxRetransmits1", //
            "async/doubleRedirectMaxRetransmits1" //
    })
    void doubleRedirectMaxRetransmits1(String endpoint) {
        int sizeBytes = 16;
        getResponse(endpoint, sizeBytes)
                .statusCode(500)
                .body(
                        CoreMatchers.either(
                                Matchers.matchesPattern("\\QReceived redirection status 307 from"
                                        + " http://localhost:\\E[0-9]+\\Q/RedirectRest/singleRedirect"
                                        + " by client doubleRedirectMaxRetransmits1,"
                                        + " but already performed maximum number 1 of allowed retransmits;"
                                        + " you may want to increase quarkus.cxf.client.doubleRedirectMaxRetransmits1.max-retransmits."
                                        + " Visited URIs: http://localhost:\\E[0-9]+\\Q/RedirectRest/doubleRedirect -> http://localhost:\\E[0-9]+\\Q/RedirectRest/singleRedirect\\E"))
                                .or(CoreMatchers.containsString("Unexpected EOF in prolog")));
    }

    @ParameterizedTest
    @ValueSource(strings = { //
            "sync/loop", //
            "async/loop" //
    })
    void redirectLoop(String endpoint) {
        int sizeBytes = 16;
        getResponse(endpoint, sizeBytes)
                .statusCode(500)
                .body(
                        CoreMatchers.either(
                                Matchers.matchesPattern(
                                        "\\QRedirect loop detected by client loop: "
                                                + "http://localhost:\\E[0-9]+\\Q/RedirectRest/loop1 -> "
                                                + "http://localhost:\\E[0-9]+\\Q/RedirectRest/loop2 -> "
                                                + "http://localhost:\\E[0-9]+\\Q/RedirectRest/loop1."
                                                + " You may want to increase quarkus.cxf.client.loop.max-same-uri\\E"))
                                .or(Matchers.matchesPattern(
                                        "\\QRedirect loop detected on Conduit '{https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test}LargeSlowServicePort.http-conduit' on 'http://localhost:\\E[0-9]+\\Q/RedirectRest/loop1'\\E")));
    }

    @ParameterizedTest
    @ValueSource(strings = { //
            "sync/maxSameUri2", //
            "async/maxSameUri2" //
    })
    void tooManySameUri(String endpoint) {
        int sizeBytes = 16;
        getResponse(endpoint, sizeBytes)
                .statusCode(500)
                .body(
                        CoreMatchers.either(
                                Matchers.matchesPattern(
                                        "\\QRedirect chain with too many same URIs http://localhost:\\E[0-9]+\\Q/RedirectRest/selfRedirect/3"
                                                + " (found 3, allowed <= 2) detected by client maxSameUri2: "
                                                + "http://localhost:\\E[0-9]+\\Q/RedirectRest/selfRedirect/3 -> "
                                                + "http://localhost:\\E[0-9]+\\Q/RedirectRest/selfRedirect/3 -> "
                                                + "http://localhost:\\E[0-9]+\\Q/RedirectRest/selfRedirect/3 -> "
                                                + "http://localhost:\\E[0-9]+\\Q/RedirectRest/selfRedirect/3. "
                                                + "You may want to increase quarkus.cxf.client.maxSameUri2.max-same-uri\\E"))
                                .or(Matchers.matchesPattern(
                                        "\\QRedirect loop detected on Conduit '{https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test}LargeSlowServicePort.http-conduit' on 'http://localhost:\\E[0-9]+\\Q/RedirectRest/selfRedirect/3'\\E")));
    }

    static ValidatableResponse getResponse(String endpoint, int sizeBytes) {
        if (endpoint.startsWith("async")) {
            /* URLConnectionHTTPConduitFactory does not support async */
            Assumptions.assumeThat(HTTPConduitImpl.findDefaultHTTPConduitImpl())
                    .isNotEqualTo(HTTPConduitImpl.URLConnectionHTTPConduitFactory);
        }

        return RestAssured.given()
                .queryParam("sizeBytes", String.valueOf(sizeBytes))
                .queryParam("delayMs", "0")
                .get("/RedirectRest/" + endpoint)
                .then();
    }

}
