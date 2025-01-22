package io.quarkiverse.cxf.it.vertx.async;

import static org.hamcrest.CoreMatchers.is;

import org.assertj.core.api.Assumptions;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkiverse.cxf.HTTPConduitImpl;
import io.quarkus.runtime.configuration.MemorySizeConverter;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
class AsyncVertxClientTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "20", // minimal
            "450k", // smaller than quarkus.cxf.retransmit-cache.threshold = 500K
            "9m" // close to max
    })
    void helloWithWsdl(String payloadSize) {
        /* URLConnectionHTTPConduitFactory does not support async */
        Assumptions.assumeThat(HTTPConduitImpl.findDefaultHTTPConduitImpl())
                .isNotEqualTo(HTTPConduitImpl.URLConnectionHTTPConduitFactory);

        final String body = body(payloadSize);
        RestAssured.given()
                .body(body)
                .post("/RestAsyncWithWsdl/helloWithWsdl")
                .then()
                .statusCode(500)
                .body(CoreMatchers.containsString(
                        "You have attempted to perform a blocking operation on an IO thread."));

    }

    @ParameterizedTest
    @ValueSource(strings = {
            "20", // minimal
            "450k", // smaller than quarkus.cxf.retransmit-cache.threshold = 500K
            "9m" // close to max
    })
    void helloWithWsdlWithBlocking(String payloadSize) {
        /* URLConnectionHTTPConduitFactory does not support async */
        Assumptions.assumeThat(HTTPConduitImpl.findDefaultHTTPConduitImpl())
                .isNotEqualTo(HTTPConduitImpl.URLConnectionHTTPConduitFactory);

        final String body = body(payloadSize);
        RestAssured.given()
                .body(body)
                .post("/RestAsyncWithWsdlWithBlocking/helloWithWsdlWithBlocking")
                .then()
                .statusCode(200)
                .body(is("Hello " + body + " from HelloWithWsdlWithBlocking"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "20", // minimal
            "450k", // smaller than quarkus.cxf.retransmit-cache.threshold = 500K
            "9m" // close to max
    })
    void helloWithWsdlWithEagerInit(String payloadSize) {
        /* URLConnectionHTTPConduitFactory does not support async */
        Assumptions.assumeThat(HTTPConduitImpl.findDefaultHTTPConduitImpl())
                .isNotEqualTo(HTTPConduitImpl.URLConnectionHTTPConduitFactory);

        final String body = body(payloadSize);
        RestAssured.given()
                .body(body)
                .post("/RestAsyncWithWsdlWithEagerInit/helloWithWsdlWithEagerInit")
                .then()
                .statusCode(200)
                .body(is("Hello " + body + " from HelloWithWsdlWithEagerInit"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "20", // minimal
            "450k", // smaller than quarkus.cxf.retransmit-cache.threshold = 500K
            "9m" // close to max
    })
    void helloWithoutWsdl(String payloadSize) {
        /* URLConnectionHTTPConduitFactory does not support async */
        Assumptions.assumeThat(HTTPConduitImpl.findDefaultHTTPConduitImpl())
                .isNotEqualTo(HTTPConduitImpl.URLConnectionHTTPConduitFactory);

        final String body = body(payloadSize);
        RestAssured.given()
                .body(body)
                .post("/RestAsyncWithoutWsdl/helloWithoutWsdl")
                .then()
                .statusCode(200)
                .body(is("Hello " + body + " from HelloWithoutWsdl"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "20", // minimal
            "450k", // smaller than quarkus.cxf.retransmit-cache.threshold = 500K
            "9m" // close to max
    })
    void helloWithoutWsdlWithBlocking(String payloadSize) {
        /* URLConnectionHTTPConduitFactory does not support async */
        Assumptions.assumeThat(HTTPConduitImpl.findDefaultHTTPConduitImpl())
                .isNotEqualTo(HTTPConduitImpl.URLConnectionHTTPConduitFactory);

        final String body = body(payloadSize);
        RestAssured.given()
                .body(body)
                .post("/RestAsyncWithoutWsdlWithBlocking/helloWithoutWsdlWithBlocking")
                .then()
                .statusCode(200)
                .body(is("Hello " + body + " from HelloWithoutWsdlWithBlocking"));
    }

    static String body(String payloadSize) {
        final MemorySizeConverter converter = new MemorySizeConverter();
        final int payloadLen = (int) converter.convert(payloadSize).asLongValue();
        final StringBuilder sb = new StringBuilder();
        while (sb.length() < payloadLen) {
            sb.append("0123456789");
        }
        sb.setLength(payloadLen);
        return sb.toString();
    }

}
