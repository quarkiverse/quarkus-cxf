package io.quarkiverse.cxf.it.http3;

import jakarta.jws.WebParam;
import jakarta.jws.WebService;

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
}
