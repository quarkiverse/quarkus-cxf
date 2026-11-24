package io.quarkiverse.cxf.it.http3;

import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.vertx.core.http.HttpConnection;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.HttpVersion;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

/**
 * A minimal SOAP service implementing {@link ProtocolEcho} that the SOAP clients of this application can call over
 * HTTP/3.
 */
@ApplicationScoped
public class Http3Peer {
    private static final Pattern BODY = Pattern.compile("<body>([^<]*)</body>");
    private static final Pattern DELAY_MS = Pattern.compile("<delayMs>([0-9]+)</delayMs>");
    private static final Pattern HOLD_MS = Pattern.compile("<holdMs>([0-9]+)</holdMs>");

    private final AtomicInteger connectionCounter = new AtomicInteger();
    /** The identifiers of the open connections through which {@link ProtocolEcho#connectionId(long)} was called */
    private final Map<HttpConnection, String> connectionIds = new ConcurrentHashMap<>();
    /** The identifiers of the connections from {@link #connectionIds} that were closed in the meantime */
    private final Set<String> closedConnectionIds = ConcurrentHashMap.newKeySet();

    void init(@Observes Router router) {
        router.post("/peer/echo").handler(this::echo);
        router.post("/peer/redirect").handler(Http3Peer::redirect);
        router.get("/peer/closedConnections")
                .handler(context -> context.response().end(String.join(",", closedConnectionIds)));
    }

    private void echo(RoutingContext context) {
        final HttpServerRequest request = context.request();
        /* Quarkus pauses the request before it reaches the routes */
        request.resume();
        request.body()
                .onSuccess(body -> {
                    final String soapRequest = body.toString();
                    final String protocol = request.version().alpnName();
                    if (request.version() != HttpVersion.HTTP_1_1 && request.getHeader("Connection") != null) {
                        request.response().setStatusCode(400)
                                .end("Connection header is forbidden in " + protocol + " requests");
                        return;
                    }
                    final Matcher delayMs = DELAY_MS.matcher(soapRequest);
                    final Matcher holdMs = HOLD_MS.matcher(soapRequest);
                    if (holdMs.find()) {
                        final String connectionId = connectionId(request.connection());
                        context.vertx().setTimer(
                                Math.max(1L, Long.parseLong(holdMs.group(1))),
                                timerId -> respond(request, "connectionIdResponse", protocol + ":" + connectionId));
                    } else if (delayMs.find()) {
                        context.vertx().setTimer(
                                Long.parseLong(delayMs.group(1)),
                                timerId -> respond(request, "delayedEchoResponse", protocol));
                    } else {
                        final Matcher echoBody = BODY.matcher(soapRequest);
                        if (echoBody.find()) {
                            respond(request, "echoResponse",
                                    protocol + ":" + echoBody.group(1).length() + clientCertificate(request));
                        } else {
                            request.response().setStatusCode(400).end("Unexpected request " + soapRequest);
                        }
                    }
                })
                .onFailure(context::fail);
    }

    /**
     * @return an identifier of the given {@code connection} that is unique within this application and stays the same
     *         as long as the given {@code connection} is open
     */
    private String connectionId(HttpConnection connection) {
        return connectionIds.computeIfAbsent(connection, cn -> {
            final String id = String.valueOf(connectionCounter.incrementAndGet());
            cn.closeHandler(v -> {
                connectionIds.remove(cn);
                closedConnectionIds.add(id);
            });
            return id;
        });
    }

    /**
     * @return a colon followed by the subject of the client certificate if the client has presented any;
     *         otherwise an empty string
     */
    private static String clientCertificate(HttpServerRequest request) {
        final SSLSession sslSession = request.connection().sslSession();
        if (sslSession != null) {
            try {
                if (sslSession.getPeerCertificates()[0] instanceof X509Certificate clientCertificate) {
                    return ":" + clientCertificate.getSubjectX500Principal().getName();
                }
            } catch (SSLPeerUnverifiedException e) {
                /* No client certificate */
            }
        }
        return "";
    }

    private static void respond(HttpServerRequest request, String wrapper, String returnValue) {
        final HttpServerResponse response = request.response();
        response.putHeader("Content-Type", "text/xml; charset=UTF-8");
        if (request.version() == HttpVersion.HTTP_2) {
            /*
             * quarkus-http3 advertises HTTP/3 only over HTTP/1.x, so we have to do it ourselves over HTTP/2.
             * The ALTSVC frame can be written only after the response head, hence the headers have to be set before.
             */
            response.writeAltSvc("h3=\":" + request.localAddress().port() + "\"");
        }
        response.end("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body>"
                + "<ns2:" + wrapper + " xmlns:ns2=\"urn:protocol-echo\"><return>" + returnValue + "</return></ns2:"
                + wrapper + ">"
                + "</soap:Body></soap:Envelope>");
    }

    /** Redirect to the URI passed via the {@code to} query parameter */
    private static void redirect(RoutingContext context) {
        final HttpServerRequest request = context.request();
        request.resume();
        request.body()
                .onSuccess(body -> request.response()
                        .setStatusCode(307)
                        .putHeader("Location", request.getParam("to"))
                        .end())
                .onFailure(context::fail);
    }
}
