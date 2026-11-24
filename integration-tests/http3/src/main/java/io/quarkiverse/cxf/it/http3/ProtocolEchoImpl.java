package io.quarkiverse.cxf.it.http3;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.jws.WebService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.xml.ws.WebServiceContext;
import jakarta.xml.ws.handler.MessageContext;

import io.quarkus.vertx.http.runtime.CurrentVertxRequest;

/**
 * A {@link ProtocolEcho} implementation exposed by Quarkus CXF, so that the service side of Quarkus CXF can be tested
 * over HTTP/3.
 * <p>
 * Unlike {@link Http3Peer}, the requests and responses of this service go through the whole server side stack of
 * Quarkus CXF, including {@code io.quarkiverse.cxf.transport.VertxHttpServletRequest} and
 * {@code io.quarkiverse.cxf.transport.VertxHttpServletResponse}.
 *
 * @since 4.0.0
 */
@WebService(serviceName = "ProtocolEcho", endpointInterface = "io.quarkiverse.cxf.it.http3.ProtocolEcho", targetNamespace = "urn:protocol-echo")
public class ProtocolEchoImpl implements ProtocolEcho {

    /** The request attribute under which the servlet API exposes the certificate chain of the client */
    private static final String CLIENT_CERTIFICATES_ATTRIBUTE = "jakarta.servlet.request.X509Certificate";

    /* Injected by the JAX-WS runtime, hence non-final */
    @Resource
    WebServiceContext wsContext;

    private final CurrentVertxRequest currentVertxRequest;
    private final ConnectionIds connectionIds;

    /**
     * @param currentVertxRequest the holder of the Vert.x request being served
     * @param connectionIds the registry of connection identifiers
     * @since 4.0.0
     */
    @Inject
    public ProtocolEchoImpl(CurrentVertxRequest currentVertxRequest, ConnectionIds connectionIds) {
        this.currentVertxRequest = currentVertxRequest;
        this.connectionIds = connectionIds;
    }

    /**
     * {@inheritDoc}
     * <p>
     * If the client has presented a certificate, a colon and the subject of that certificate are appended to the
     * result, in the same way as {@link Http3Peer} does it.
     *
     * @since 4.0.0
     */
    @Override
    public String echo(String body) {
        return request().getProtocol() + ":" + body.length() + clientCertificate(request());
    }

    /**
     * {@inheritDoc}
     *
     * @since 4.0.0
     */
    @Override
    public String delayedEcho(long delayMs) {
        sleep(delayMs);
        return request().getProtocol();
    }

    /**
     * {@inheritDoc}
     *
     * @since 4.0.0
     */
    @Override
    public String connectionId(long holdMs) {
        final String connectionId = connectionIds.id(currentVertxRequest.getCurrent().request().connection());
        sleep(holdMs);
        return request().getProtocol() + ":" + connectionId;
    }

    /**
     * {@inheritDoc}
     *
     * @since 4.0.0
     */
    @Override
    public String generate(int length) {
        if (length < 0) {
            throw new IllegalArgumentException("length must not be negative: " + length);
        }
        return request().getProtocol() + ":" + "x".repeat(length);
    }

    private HttpServletRequest request() {
        return (HttpServletRequest) wsContext.getMessageContext().get(MessageContext.SERVLET_REQUEST);
    }

    /**
     * @param request the request to get the client certificate from
     * @return a colon followed by the subject of the client certificate if the client has presented any;
     *         otherwise an empty string
     */
    private static String clientCertificate(HttpServletRequest request) {
        if (request.getAttribute(CLIENT_CERTIFICATES_ATTRIBUTE) instanceof Certificate[] certificates
                && certificates.length > 0
                && certificates[0] instanceof X509Certificate clientCertificate) {
            return ":" + clientCertificate.getSubjectX500Principal().getName();
        }
        return "";
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
