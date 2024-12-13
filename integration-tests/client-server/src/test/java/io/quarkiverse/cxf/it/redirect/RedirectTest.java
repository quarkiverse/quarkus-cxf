package io.quarkiverse.cxf.it.redirect;

import java.util.concurrent.ExecutionException;

import org.assertj.core.api.Assumptions;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkiverse.cxf.HTTPConduitImpl;
import io.quarkiverse.cxf.it.large.slow.LargeSlowServiceImpl;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;

@QuarkusTest
class RedirectTest {

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

            "sync/selfRedirect", //
            "async/selfRedirect" //

    })
    void redirect(String endpoint) throws InterruptedException, ExecutionException {
        int sizeBytes = 16; // smallish, suits single buffer in
                            // VertxHttpClientHTTPConduit.RequestBodyHandler.bodyRecorder
        getResponse(endpoint, sizeBytes)
                .statusCode(200)
                .body(Matchers.is(LargeSlowServiceImpl.largeString(sizeBytes)));

        sizeBytes = 9 * 1024; // biggish, forces multiple buffers in
                              // VertxHttpClientHTTPConduit.RequestBodyHandler.bodyRecorder
        getResponse(endpoint, sizeBytes)
                .statusCode(200)
                .body(Matchers.is(LargeSlowServiceImpl.largeString(sizeBytes)));
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
                .body(CoreMatchers.containsString("Unexpected EOF in prolog"));
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
                .body(CoreMatchers.containsString("Unexpected EOF in prolog"));
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
                .body(CoreMatchers.allOf(CoreMatchers.containsString(
                        "Redirect loop detected on Conduit '{https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test}LargeSlowServicePort.http-conduit' (with http.redirect.max.same.uri.count = 0):"),
                        CoreMatchers.containsString(
                                "You may want to increase quarkus.cxf.client.\"client-name\".max-retransmits in application.properties where \"client-name\" is the name of your client in application.properties")));
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
