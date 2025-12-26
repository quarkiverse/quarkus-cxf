package io.quarkiverse.cxf.vertx.http.client;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

import org.apache.cxf.transports.http.configuration.ConnectionType;
import org.assertj.core.api.Assertions;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hamcrest.CoreMatchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.HttpServerOptionsCustomizer;
import io.restassured.RestAssured;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpVersion;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

@Certificates(baseDir = "target/classes", //
        certificates = @Certificate( //
                name = "localhost", //
                password = "secret", //
                formats = { Format.PKCS12 }))
public class KeepAliveTimeoutTest {
    private static final Pattern REQUEST_PATTERN = Pattern.compile("<arg0>([^<]*)</arg0>");

    @RegisterExtension
    public static final QuarkusUnitTest test = createTest();

    //
    //    @WebService(serviceName = "EchoHeaders")
    //    public static class EchoHeadersServiceImpl implements EchoHeadersService {
    //
    //        @Resource
    //        WebServiceContext wsContext;
    //
    //        @Override
    //        public String getRequestHeaders(String keys) {
    //            HttpServletRequest req = (HttpServletRequest) wsContext.getMessageContext().get(MessageContext.SERVLET_REQUEST);
    //
    //            return Stream.of(keys.split(","))
    //                    .map(k -> k + ":" + req.getHeader(k))
    //                    .collect(Collectors.joining(","));
    //        }
    //    }

    static QuarkusUnitTest createTest() {

        final String http1xBaseUri = "http://localhost:8081";
        final Map<HttpVersion, String> baseUris = Map.of(
                HttpVersion.HTTP_1_0, http1xBaseUri,
                HttpVersion.HTTP_1_1, http1xBaseUri,
                HttpVersion.HTTP_2, "https://localhost:8444");

        QuarkusUnitTest result = new QuarkusUnitTest()
                .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                        .addClasses(EchoHeadersService.class))
                .overrideConfigKey("quarkus.tls.key-store.p12.path", "localhost-keystore.p12")
                .overrideConfigKey("quarkus.tls.key-store.p12.password", "secret")

                .overrideConfigKey("quarkus.tls.client-pkcs12.trust-store.p12.path", "localhost-truststore.p12")
                .overrideConfigKey("quarkus.tls.client-pkcs12.trust-store.p12.password", "secret")

                //.overrideConfigKey("quarkus.cxf.endpoint.\"/echoHeaders\".implementor", EchoHeadersServiceImpl.class.getName())
                //.overrideConfigKey("quarkus.cxf.endpoint.\"/echoHeaders\".logging.enabled", "pretty")
                .overrideConfigKey("qcxf.baseUri", http1xBaseUri);

        for (HttpVersion v : HttpVersion.values()) {
            final String baseUri = baseUris.get(v);
            for (String connectionValue : List.of("keep-alive", "close")) {
                String k = "quarkus.cxf.client." + connectionValue + "-client_" + v.name();
                if (baseUri.startsWith("https://")) {
                    result.overrideConfigKey(k + ".tls-configuration-name", "client-pkcs12");
                }
                result.overrideConfigKey(k + ".client-endpoint-url", baseUri + "/echoHeaders/" + v.name())
                        .overrideConfigKey(k + ".service-interface", EchoHeadersService.class.getName())
                        /* This scenario works only with vert.x client */
                        .overrideConfigKey(k + ".http-conduit-factory", "VertxHttpClientHTTPConduitFactory")
                        .overrideConfigKey(k + ".connection", connectionValue)
                        .overrideConfigKey(k + ".version", plainVersion(v))
                        .overrideConfigKey(k + ".logging.enabled", "pretty");
            }

            String connectionValue = "keep-alive";
            String k = "quarkus.cxf.client." + connectionValue + "-with-timeout-client_" + v.name();
            if (baseUri.startsWith("https://")) {
                result.overrideConfigKey(k + ".tls-configuration-name", "client-pkcs12");
            }
            result.overrideConfigKey(k + ".client-endpoint-url", baseUri + "/echoHeaders/" + v.name())
                    .overrideConfigKey(k + ".service-interface", EchoHeadersService.class.getName())
                    /* This scenario works only with vert.x client */
                    .overrideConfigKey(k + ".http-conduit-factory", "VertxHttpClientHTTPConduitFactory")
                    .overrideConfigKey(k + ".connection", connectionValue)
                    .overrideConfigKey(k + ".vertx." + (v == HttpVersion.HTTP_2 ? "http2-" : "") + "keep-alive-timeout", "1")
                    .overrideConfigKey(k + ".version", plainVersion(v))
                    .overrideConfigKey(k + ".logging.enabled", "pretty");

        }

        return result;

    }

    //
    //    @WebService(serviceName = "EchoHeaders")
    //    public static class EchoHeadersServiceImpl implements EchoHeadersService {
    //
    //        @Resource
    //        WebServiceContext wsContext;
    //
    //        @Override
    //        public String getRequestHeaders(String keys) {
    //            HttpServletRequest req = (HttpServletRequest) wsContext.getMessageContext().get(MessageContext.SERVLET_REQUEST);
    //
    //            return Stream.of(keys.split(","))
    //                    .map(k -> k + ":" + req.getHeader(k))
    //                    .collect(Collectors.joining(","));
    //        }
    //    }

    static String plainVersion(HttpVersion v) {
        return switch (v) {
            case HTTP_1_0: {
                yield "1.0";
            }
            case HTTP_1_1: {
                yield "1.1";
            }
            case HTTP_2: {
                yield "2";
            }
            default:
                throw new IllegalArgumentException("Unexpected HTTP version: " + v);
        };
    }

    @CXFClient("keep-alive-client_HTTP_1_0")
    EchoHeadersService keepAliveClient_HTTP_1_0;

    @CXFClient("keep-alive-client_HTTP_1_1")
    EchoHeadersService keepAliveClient_HTTP_1_1;

    @CXFClient("keep-alive-client_HTTP_2")
    EchoHeadersService keepAliveClient_HTTP_2;

    @CXFClient("close-client_HTTP_1_0")
    EchoHeadersService closeClient_HTTP_1_0;

    @CXFClient("close-client_HTTP_1_1")
    EchoHeadersService closeClient_HTTP_1_1;

    @CXFClient("close-client_HTTP_2")
    EchoHeadersService closeClient_HTTP_2;

    @CXFClient("keep-alive-with-timeout-client_HTTP_1_0")
    EchoHeadersService keepAliveWithTimeoutClient_HTTP_1_0;

    @CXFClient("keep-alive-with-timeout-client_HTTP_1_1")
    EchoHeadersService keepAliveWithTimeoutClient_HTTP_1_1;

    @CXFClient("keep-alive-with-timeout-client_HTTP_2")
    EchoHeadersService keepAliveWithTimeoutClient_HTTP_2;

    @ConfigProperty(name = "qcxf.baseUri")
    String baseUri;

    @Test
    void keepAliveClient_HTTP_1_0() {
        keepAlive(keepAliveClient_HTTP_1_0, ConnectionType.KEEP_ALIVE.value());
    }

    @Test
    void keepAliveClient_HTTP_1_1() {
        keepAlive(keepAliveClient_HTTP_1_1, ConnectionType.KEEP_ALIVE.value());
    }

    @Test
    void keepAliveClient_HTTP_2() {
        keepAlive(keepAliveClient_HTTP_2, "null" /* the client should not send the connection header */);
    }

    @Test
    void closeClient_HTTP_1_0() {
        close(closeClient_HTTP_1_0, HttpVersion.HTTP_1_0, ConnectionType.CLOSE.value());
    }

    @Test
    void closeClient_HTTP_1_1() {
        close(closeClient_HTTP_1_1, HttpVersion.HTTP_1_1, ConnectionType.CLOSE.value());
    }

    @Test
    void closeClient_HTTP_2() {
        /* connection: close behaves the same as connection: keep-alive with HTTP/2 because the config value is ignored */
        keepAlive(closeClient_HTTP_2, "null" /* the client should not send the connection header */);
    }

    @Test
    void keepAliveWithTimeoutClient_HTTP_1_0() {
        /* When .vertx.keep-alive-timeout is set 1 then we should observe the connection being closed after 1s */
        close(keepAliveWithTimeoutClient_HTTP_1_0, HttpVersion.HTTP_1_0, ConnectionType.KEEP_ALIVE.value());
    }

    @Test
    void keepAliveWithTimeoutClient_HTTP_1_1() {
        /* When .vertx.keep-alive-timeout is set 1 then we should observe the connection being closed after 1s */
        close(keepAliveWithTimeoutClient_HTTP_1_1, HttpVersion.HTTP_1_1, ConnectionType.KEEP_ALIVE.value());
    }

    @Test
    void keepAliveWithTimeoutClient_HTTP_2() {
        /* When .vertx.keep-alive-timeout is set 1 then we should observe the connection being closed after 1s */
        close(keepAliveWithTimeoutClient_HTTP_2, HttpVersion.HTTP_2, "null");
    }

    static void keepAlive(EchoHeadersService client, String expectedConnectionValue) {
        final String cnId;
        {
            Map<String, String> resp = toMap(client.getRequestHeaders("connection"));
            Assertions.assertThat(resp).containsOnlyKeys("cnId", "connection");
            cnId = resp.get("cnId");
            Assertions.assertThat(cnId).isNotBlank();
            Assertions.assertThat(resp.get("connection")).isEqualToIgnoringCase(expectedConnectionValue);
        }

        /* Do it again and make sure the connection was reused */
        {
            Map<String, String> resp = toMap(client.getRequestHeaders("connection"));
            Assertions.assertThat(resp).containsOnlyKeys("cnId", "connection");
            Assertions.assertThat(resp.get("cnId")).isEqualTo(cnId);
            Assertions.assertThat(resp.get("connection")).isEqualToIgnoringCase(expectedConnectionValue);
        }
    }

    void close(EchoHeadersService client, HttpVersion httpVersion, String expectedConnectionValue) {
        Map<String, String> resp = toMap(client.getRequestHeaders("connection"));
        Assertions.assertThat(resp).containsOnlyKeys("cnId", "connection");
        final String cnId = resp.get("cnId");
        Assertions.assertThat(cnId).isNotBlank();
        Assertions.assertThat(resp.get("connection")).isEqualToIgnoringCase(expectedConnectionValue);

        /* Await the connection close */
        RestAssured.get(baseUri + "/closedConnections/" + httpVersion.name())
                .then()
                .statusCode(200)
                .body(CoreMatchers.is(cnId));
    }

    static Map<String, String> toMap(String requestHeaders) {
        Map<String, String> result = new LinkedHashMap<>();
        Stream.of(requestHeaders.split(","))
                .forEach(kv -> result.put(
                        kv.split(":")[0],
                        kv.split(":")[1]));
        return Collections.unmodifiableMap(result);
    }

    void init(@Observes Router router) {

        Map<HttpVersion, BlockingQueue<String>> map = new LinkedHashMap<>();
        for (HttpVersion v : HttpVersion.values()) {
            map.put(v, new LinkedBlockingQueue<>());
        }
        final Map<HttpVersion, BlockingQueue<String>> closedConnections = Collections.unmodifiableMap(map);

        router.route().handler(BodyHandler.create());

        router.post("/echoHeaders/:httpVersion").handler(context -> {
            final HttpVersion expectedHttpVersion = HttpVersion.valueOf(context.pathParam("httpVersion"));
            String msg = "Expected " + expectedHttpVersion + "; found " + context.request().version();
            if (expectedHttpVersion != context.request().version()) {
                context.response()
                        .setStatusCode(505)
                        .end(msg);
            }
            final String body = context.body().asString();
            //log.infof("Received body: %s", body);
            final Matcher m = REQUEST_PATTERN.matcher(body);
            if (!m.find()) {
                context.response()
                        .setStatusCode(500)
                        .end("Body must match " + REQUEST_PATTERN.pattern());
            }
            final String keys = m.group(1);

            final String keyVals = Stream.of(keys.split(","))
                    .map(k -> k + ":" + context.request().getHeader(k))
                    .collect(Collectors.joining(","));

            HttpConnection cn = context.request().connection();
            final String cnId = String.valueOf(System.identityHashCode(cn));
            cn.closeHandler(v -> closedConnections.get(expectedHttpVersion).add(cnId));

            context.response()
                    .putHeader("Content-Type", "text/xml; charset=UTF-8")
                    .setStatusCode(200).end(
                            """
                                    <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                                        <soap:Body>
                                            <ns2:getRequestHeadersResponse xmlns:ns2="http://client.http.vertx.cxf.quarkiverse.io/">
                                              <return>%s,cnId:%s</return>
                                            </ns2:getRequestHeadersResponse>
                                        </soap:Body>
                                    </soap:Envelope>
                                    """
                                    .formatted(keyVals, cnId));
        });
        router.get("/closedConnections/:httpVersion").blockingHandler(context -> {
            final HttpVersion expectedHttpVersion = HttpVersion.valueOf(context.pathParam("httpVersion"));
            try {
                context.response()
                        .setStatusCode(200)
                        .end(String.valueOf(closedConnections.get(expectedHttpVersion).take()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

    }

    @WebService
    public interface EchoHeadersService {

        @WebMethod
        String getRequestHeaders(String keys);

    }
    //
    //    @WebService(serviceName = "EchoHeaders")
    //    public static class EchoHeadersServiceImpl implements EchoHeadersService {
    //
    //        @Resource
    //        WebServiceContext wsContext;
    //
    //        @Override
    //        public String getRequestHeaders(String keys) {
    //            HttpServletRequest req = (HttpServletRequest) wsContext.getMessageContext().get(MessageContext.SERVLET_REQUEST);
    //
    //            return Stream.of(keys.split(","))
    //                    .map(k -> k + ":" + req.getHeader(k))
    //                    .collect(Collectors.joining(","));
    //        }
    //    }

    @ApplicationScoped
    public static class Http2OnlyServerOptionsCustomizer implements HttpServerOptionsCustomizer {

        @Override
        public void customizeHttpsServer(HttpServerOptions options) {
            // Ensure ALPN is on and only advertise HTTP/2
            options.setUseAlpn(true);
            options.setAlpnVersions(List.of(HttpVersion.HTTP_2, HttpVersion.HTTP_1_1, HttpVersion.HTTP_1_0));
        }
    }
}
