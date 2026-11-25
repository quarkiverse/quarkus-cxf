package io.quarkiverse.cxf.it.http3;

import jakarta.jws.WebParam;
import jakarta.jws.WebService;

/**
 * A service telling the caller through which HTTP protocol the request came.
 * <p>
 * All operations name the protocol in the form returned by {@code jakarta.servlet.ServletRequest.getProtocol()},
 * that is {@code HTTP/1.0}, {@code HTTP/1.1}, {@code HTTP/2.0} or {@code HTTP/3.0}.
 *
 * @since 4.0.0
 */
@WebService(targetNamespace = "urn:protocol-echo")
public interface ProtocolEcho {

    /**
     * @param body any text
     * @return the HTTP protocol through which the request came, followed by a colon and the length of the given {@code body}
     */
    String echo(@WebParam(name = "body") String body);

    /**
     * @param delayMs the number of milliseconds to wait before returning
     * @return the HTTP protocol through which the request came
     */
    String delayedEcho(@WebParam(name = "delayMs") long delayMs);

    /**
     * @param holdMs the number of milliseconds to wait before returning
     * @return the HTTP protocol through which the request came, followed by a colon and the identifier of the connection
     *         through which the request came
     */
    String connectionId(@WebParam(name = "holdMs") long holdMs);

    /**
     * Generates a response of the requested size.
     * <p>
     * Implemented only by {@link ProtocolEchoImpl}; {@link Http3Peer} rejects this operation with HTTP status 400.
     *
     * @param length the number of {@code x} characters to return; must not be negative
     * @return the HTTP protocol through which the request came, followed by a colon and {@code length} {@code x}
     *         characters
     * @throws IllegalArgumentException if {@code length} is negative
     * @since 4.0.0
     */
    String generate(@WebParam(name = "length") int length);
}
