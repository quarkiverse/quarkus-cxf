package io.quarkiverse.cxf.it.http3;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;

/**
 * A minimal SOAP service implementing {@link ProtocolEcho#echo(String)} that is available only over HTTP/1.1 and that
 * advertises {@link Http3Peer} as its HTTP/3 alternative via the {@code Alt-Svc} response header.
 * <p>
 * The HTTP server of the application cannot be used for testing the discovery of HTTP/3 over HTTP/1.1, because it
 * supports HTTP/2, which is what a SOAP client having {@code vertx.http-versions = 3,2,1.1} would always negotiate with
 * it.
 *
 * @since 4.0.0
 */
@ApplicationScoped
public class Http11Peer {
    private static final Pattern BODY = Pattern.compile("<body>([^<]*)</body>");

    @Inject
    Vertx vertx;

    @Inject
    TlsConfigurationRegistry tlsRegistry;

    @ConfigProperty(name = "http11-peer.port")
    int port;

    /** The port on which {@link Http3Peer} is available over HTTP/3 */
    @ConfigProperty(name = "http11-peer.http3-port")
    int http3Port;

    private volatile HttpServer server;

    void start(@Observes StartupEvent event) throws Exception {
        final HttpServerOptions options = new HttpServerOptions()
                .setSsl(true)
                /* No ALPN, hence HTTP/1.1 only */
                .setUseAlpn(false)
                .setKeyCertOptions(tlsRegistry.get("localhost").orElseThrow().getKeyStoreOptions());
        server = vertx.createHttpServer(options)
                .requestHandler(this::echo)
                .listen(port)
                .toCompletionStage().toCompletableFuture().get(30, TimeUnit.SECONDS);
    }

    void stop(@Observes ShutdownEvent event) throws Exception {
        final HttpServer srv = server;
        if (srv != null) {
            srv.close().toCompletionStage().toCompletableFuture().get(30, TimeUnit.SECONDS);
        }
    }

    private void echo(HttpServerRequest request) {
        request.body()
                .onSuccess(body -> {
                    final Matcher echoBody = BODY.matcher(body.toString());
                    if (!"/peer/echo".equals(request.path()) || !echoBody.find()) {
                        request.response().setStatusCode(400).end("Unexpected request " + request.path() + " " + body);
                        return;
                    }
                    request.response()
                            .putHeader("Content-Type", "text/xml; charset=UTF-8")
                            .putHeader("Alt-Svc", "h3=\":" + http3Port + "\"")
                            .end("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body>"
                                    + "<ns2:echoResponse xmlns:ns2=\"urn:protocol-echo\"><return>"
                                    + request.version().alpnName() + ":" + echoBody.group(1).length()
                                    + "</return></ns2:echoResponse>"
                                    + "</soap:Body></soap:Envelope>");
                })
                .onFailure(e -> request.response().setStatusCode(500).end(String.valueOf(e)));
    }
}
