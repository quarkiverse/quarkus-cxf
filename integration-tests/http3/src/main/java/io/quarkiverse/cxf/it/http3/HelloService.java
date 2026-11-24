package io.quarkiverse.cxf.it.http3;

import jakarta.jws.WebParam;
import jakarta.jws.WebService;

/**
 * A greeting service used as an example in the documentation of HTTP/3 for SOAP services.
 *
 * @since 4.0.0
 */
@WebService(targetNamespace = "urn:hello")
public interface HelloService {

    /**
     * @param person the name of the person to greet
     * @return a greeting for the given {@code person} mentioning the HTTP version through which the request came
     * @since 4.0.0
     */
    String hello(@WebParam(name = "person") String person);
}
