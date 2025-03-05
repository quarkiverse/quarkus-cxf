package io.quarkiverse.cxf.it.vertx.async;

import static org.hamcrest.CoreMatchers.is;

import java.time.Duration;

import org.assertj.core.api.Assumptions;
import org.jboss.logging.Logger;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkiverse.cxf.HTTPConduitImpl;
import io.quarkiverse.cxf.test.QuarkusCxfClientTestUtil;
import io.quarkus.runtime.configuration.MemorySizeConverter;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
class AsyncVertxClientTest {
    private static final Logger log = Logger.getLogger(AsyncVertxClientTest.class);

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
        log.infof("Starting AsyncVertxClientTest.helloWithWsdl with %s body", payloadSize);

        assert200("/RestAsyncWithWsdl/helloWithWsdl", payloadSize,
                "Hello from HelloWithWsdl ");

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
        log.infof("Starting AsyncVertxClientTest.helloWithWsdlWithBlocking with %s body", payloadSize);
        assert200("/RestAsyncWithWsdlWithBlocking/helloWithWsdlWithBlocking", payloadSize,
                "Hello from HelloWithWsdlWithBlocking ");
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
        log.infof("Starting AsyncVertxClientTest.helloWithWsdlWithEagerInit with %s body", payloadSize);
        assert200("/RestAsyncWithWsdlWithEagerInit/helloWithWsdlWithEagerInit", payloadSize,
                "Hello from HelloWithWsdlWithEagerInit ");
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
        log.infof("Starting AsyncVertxClientTest.helloWithoutWsdl with %s body", payloadSize);
        assert200("/RestAsyncWithoutWsdl/helloWithoutWsdl", payloadSize, "Hello from HelloWithoutWsdl ");
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
        log.infof("Starting AsyncVertxClientTest.helloWithoutWsdlWithBlocking with %s body", payloadSize);
        assert200("/RestAsyncWithoutWsdlWithBlocking/helloWithoutWsdlWithBlocking", payloadSize,
                "Hello from HelloWithoutWsdlWithBlocking ");
    }

    static void assert200(String endpoint, String payloadSize, String expectedBodyPrefix) {
        QuarkusCxfClientTestUtil.printThreadDumpAtTimeout(
                () -> {
                    final String body = body(payloadSize);
                    RestAssured.given()
                            .body(body)
                            .post(endpoint)
                            .then()
                            .statusCode(200)
                            .body(is(expectedBodyPrefix + body));
                    return null;
                },
                Duration.ofSeconds(5),
                log::info);
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
