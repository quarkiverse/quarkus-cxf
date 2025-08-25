package io.quarkiverse.cxf.it.vertx.async;

import java.time.Duration;

import org.apache.http.params.CoreConnectionPNames;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.Assumptions;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import io.quarkiverse.cxf.HTTPConduitImpl;
import io.quarkiverse.cxf.test.QuarkusCxfClientTestUtil;
import io.quarkus.runtime.configuration.MemorySizeConverter;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.response.Response;

@QuarkusTest
class AsyncVertxClientTest {
    private static final Logger log = Logger.getLogger(AsyncVertxClientTest.class);

    @Test
    void helloWithWsdl20() {
        helloWithoutWsdl("20"); // minimal
    }

    @Test
    void helloWithWsdl450k() {
        helloWithoutWsdl("450k"); // smaller than quarkus.cxf.retransmit-cache.threshold = 500K
    }

    @Test
    void helloWithWsdl9m() {
        helloWithoutWsdl("9m"); // close to max
    }

    void helloWithWsdl(String payloadSize) {
        /* URLConnectionHTTPConduitFactory does not support async */
        Assumptions.assumeThat(HTTPConduitImpl.findDefaultHTTPConduitImpl())
                .isNotEqualTo(HTTPConduitImpl.URLConnectionHTTPConduitFactory);
        log.infof("Starting AsyncVertxClientTest.helloWithWsdl%s", payloadSize);

        assert200("/RestAsyncWithWsdl/helloWithWsdl", payloadSize,
                "Hello from HelloWithWsdl ");

    }

    @Test
    void helloWithWsdlWithBlocking20() {
        helloWithWsdlWithBlocking("20"); // minimal
    }

    @Test
    void helloWithWsdlWithBlocking450k() {
        helloWithWsdlWithBlocking("450k"); // smaller than quarkus.cxf.retransmit-cache.threshold = 500K
    }

    @Test
    void helloWithWsdlWithBlocking9m() {
        helloWithWsdlWithBlocking("9m"); // close to max
    }

    void helloWithWsdlWithBlocking(String payloadSize) {
        /* URLConnectionHTTPConduitFactory does not support async */
        Assumptions.assumeThat(HTTPConduitImpl.findDefaultHTTPConduitImpl())
                .isNotEqualTo(HTTPConduitImpl.URLConnectionHTTPConduitFactory);
        log.infof("Starting AsyncVertxClientTest.helloWithWsdlWithBlocking%s", payloadSize);
        assert200("/RestAsyncWithWsdlWithBlocking/helloWithWsdlWithBlocking", payloadSize,
                "Hello from HelloWithWsdlWithBlocking ");
    }

    @Test
    void helloWithWsdlWithEagerInit20() {
        helloWithWsdlWithEagerInit("20"); // minimal
    }

    @Test
    void helloWithWsdlWithEagerInit450k() {
        helloWithWsdlWithEagerInit("450k"); // smaller than quarkus.cxf.retransmit-cache.threshold = 500K
    }

    @Test
    void helloWithWsdlWithEagerInit9m() {
        helloWithWsdlWithEagerInit("9m"); // close to max
    }

    void helloWithWsdlWithEagerInit(String payloadSize) {
        /* URLConnectionHTTPConduitFactory does not support async */
        Assumptions.assumeThat(HTTPConduitImpl.findDefaultHTTPConduitImpl())
                .isNotEqualTo(HTTPConduitImpl.URLConnectionHTTPConduitFactory);
        log.infof("Starting AsyncVertxClientTest.helloWithWsdlWithEagerInit%s", payloadSize);
        assert200("/RestAsyncWithWsdlWithEagerInit/helloWithWsdlWithEagerInit", payloadSize,
                "Hello from HelloWithWsdlWithEagerInit ");
    }

    @Test
    void helloWithoutWsdl20() {
        helloWithoutWsdl("20"); // minimal
    }

    @Test
    void helloWithoutWsdl450k() {
        helloWithoutWsdl("450k"); // smaller than quarkus.cxf.retransmit-cache.threshold = 500K
    }

    @Test
    void helloWithoutWsdl9m() {
        helloWithoutWsdl("9m"); // close to max
    }

    void helloWithoutWsdl(String payloadSize) {
        /* URLConnectionHTTPConduitFactory does not support async */
        Assumptions.assumeThat(HTTPConduitImpl.findDefaultHTTPConduitImpl())
                .isNotEqualTo(HTTPConduitImpl.URLConnectionHTTPConduitFactory);
        log.infof("Starting AsyncVertxClientTest.helloWithoutWsdl%s", payloadSize);
        assert200("/RestAsyncWithoutWsdl/helloWithoutWsdl", payloadSize, "Hello from HelloWithoutWsdl ");
    }

    @Test
    void helloWithoutWsdlWithBlocking20() {
        helloWithoutWsdlWithBlocking("20"); // minimal
    }

    @Test
    void helloWithoutWsdlWithBlocking450k() {
        helloWithoutWsdlWithBlocking("450k"); // smaller than quarkus.cxf.retransmit-cache.threshold = 500K
    }

    @Test
    void helloWithoutWsdlWithBlocking9m() {
        helloWithoutWsdlWithBlocking("9m"); // close to max
    }

    void helloWithoutWsdlWithBlocking(String payloadSize) {
        /* URLConnectionHTTPConduitFactory does not support async */
        Assumptions.assumeThat(HTTPConduitImpl.findDefaultHTTPConduitImpl())
                .isNotEqualTo(HTTPConduitImpl.URLConnectionHTTPConduitFactory);
        log.infof("Starting AsyncVertxClientTest.helloWithoutWsdlWithBlocking%s", payloadSize);
        assert200("/RestAsyncWithoutWsdlWithBlocking/helloWithoutWsdlWithBlocking", payloadSize,
                "Hello from HelloWithoutWsdlWithBlocking ");
    }

    static void assert200(String endpoint, String payloadSize, String expectedBodyPrefix) {
        QuarkusCxfClientTestUtil.printThreadDumpAtTimeout(
                () -> {
                    final String body = body(payloadSize);
                    final Response resp = RestAssured.given()
                            .config(
                                    RestAssuredConfig.config()
                                            .httpClient(HttpClientConfig.httpClientConfig()
                                                    .setParam(CoreConnectionPNames.SO_TIMEOUT, 70_000)))
                            .body(body)
                            .post(endpoint)
                            .then().extract().response();
                    if (resp.statusCode() != 200) {
                        Assertions.assertThat(resp.statusCode())
                                .withFailMessage("Expected 200, got " + resp.statusCode() + ":\n" + resp.body().asString())
                                .isEqualTo(200);
                    }
                    Assertions.assertThat(resp.body().asString()).isEqualTo(expectedBodyPrefix + body);
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
