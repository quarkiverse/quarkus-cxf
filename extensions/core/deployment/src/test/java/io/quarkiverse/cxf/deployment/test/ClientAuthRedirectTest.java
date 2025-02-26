package io.quarkiverse.cxf.deployment.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

import org.assertj.core.api.Assertions;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hamcrest.CoreMatchers;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class ClientAuthRedirectTest {
    private static final Logger log = Logger.getLogger(ClientAuthRedirectTest.class);
    private static final String USERNAME = "test";
    private static final String PASSWORD = "secret";
    private static final String NONCE = "some_nonce_value";
    private static final Pattern REQUEST_PATTERN = Pattern.compile("<arg0>([^<]*)</arg0>");

    @RegisterExtension
    public static final QuarkusUnitTest test = createTest();

    private static QuarkusUnitTest createTest() {
        final Vertx vertx = Vertx.vertx();

        final Map<String, List<Integer>> responseCodesByPerson = new ConcurrentHashMap<>();

        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.get("/responseCodes/:person").handler(context -> {
            final List<Integer> resps = responseCodesByPerson
                    .computeIfAbsent(context.pathParam("person"), k -> new ArrayList<>());
            context.response()
                    .setStatusCode(200)
                    .end(resps.stream()
                            .map(Object::toString)
                            .collect(Collectors.joining(",")));

        });
        router.post("/authRedirectService").handler(context -> {
            final String body = context.body().asString();
            log.infof("Received body: %s", body);
            final Matcher m = REQUEST_PATTERN.matcher(body);
            if (!m.find()) {
                context.response()
                        .setStatusCode(500)
                        .end("Body must match " + REQUEST_PATTERN.pattern());
            }
            final String person = m.group(1);
            final List<Integer> previousResponses = responseCodesByPerson.computeIfAbsent(person, k -> new ArrayList<>());
            if (previousResponses.isEmpty()) {
                log.infof("First request for person: %s -> challening with 401 and WWW-Authenticate", person);
                previousResponses.add(401);
                context.response()
                        .setStatusCode(401)
                        .putHeader("WWW-Authenticate",
                                "Digest realm=\"TestRealm\", nonce=\"" + NONCE + "\", algorithm=MD5, qop=\"auth\"")
                        .end("Unauthorized");
                return;
            } else {
                log.infof("Second request for person: %s -> sending 200 and response body", person);
                final String authHeader = context.request().getHeader("Authorization");
                final String prefix = "Digest ";

                if (authHeader == null) {
                    context.response()
                            .setStatusCode(400)
                            .end("Missing Authorization header");
                    return;
                } else if (!authHeader.startsWith(prefix)) {
                    context.response()
                            .setStatusCode(400)
                            .end("Authorization header must start with " + prefix);
                    return;
                }

                final Map<String, String> map = parseDigestHeader(authHeader.substring(prefix.length()));
                if (!USERNAME.equals(map.get("username")) || !NONCE.equals(map.get("nonce"))) {
                    context.response()
                            .setStatusCode(400)
                            .end("Bad username " + map.get("username") + " or nonce " + map.get("nonce"));
                    return;
                }

                /*
                 * We don't completely check whether the digest is valid
                 * Do not use this in production!
                 */

                previousResponses.add(200);
                context.response()
                        .putHeader("Content-Type", "text/xml; charset=UTF-8")
                        .setStatusCode(200).end(
                                """
                                        <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                                            <soap:Body>
                                                <ns2:helloResponse xmlns:ns2="http://test.deployment.cxf.quarkiverse.io/">
                                                    <return>Hello authenticated %s</return>
                                                </ns2:helloResponse>
                                            </soap:Body>
                                        </soap:Envelope>
                                        """
                                        .formatted(person));
            }
        });

        final HttpServer server = vertx.createHttpServer(new HttpServerOptions())
                .requestHandler(router)
                .listen(-2)
                .toCompletionStage()
                .toCompletableFuture()
                .join();

        return new QuarkusUnitTest()
                .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                        .addClasses(HelloService.class))

                /* Client */
                .overrideConfigKey("qcxf.test.baseUri", "http://localhost:" + server.actualPort())
                .overrideConfigKey("quarkus.cxf.client.authRedirect.client-endpoint-url",
                        "http://localhost:" + server.actualPort() + "/authRedirectService")
                .overrideConfigKey("quarkus.cxf.client.authRedirect.service-interface", HelloService.class.getName())
                .overrideConfigKey("quarkus.cxf.client.authRedirect.logging.enabled", "true")

                /* Authorization */
                .overrideConfigKey("quarkus.cxf.client.authRedirect.auth.username", USERNAME)
                .overrideConfigKey("quarkus.cxf.client.authRedirect.auth.password", PASSWORD)
                .overrideConfigKey("quarkus.cxf.client.authRedirect.auth.scheme", "Digest")

                .setAfterAllCustomizer(() -> {
                    vertx.close().toCompletionStage().toCompletableFuture().join();
                });
    }

    @CXFClient("authRedirect")
    HelloService authRedirect;

    @ConfigProperty(name = "qcxf.test.baseUri")
    String baseUri;

    @Test
    void authRedirect() {
        Assertions.assertThat(authRedirect.hello("foo")).isEqualTo("Hello authenticated foo");
        /* Make sure that the server responded 401 first and that the auth retransmit really happened */
        RestAssured.get(baseUri + "/responseCodes/foo")
                .then()
                .statusCode(200)
                .body(CoreMatchers.is("401,200"));
    }

    @WebService(serviceName = "HelloService", targetNamespace = "http://test.deployment.cxf.quarkiverse.io/")
    public interface HelloService {

        @WebMethod
        String hello(String person);

    }

    static Map<String, String> parseDigestHeader(String header) {
        Map<String, String> map = new ConcurrentHashMap<>();
        String[] parts = header.split(", ");
        for (String part : parts) {
            String[] keyValue = part.split("=", 2);
            if (keyValue.length == 2) {
                map.put(keyValue[0], keyValue[1].replace("\"", ""));
            }
        }
        return map;
    }

}
