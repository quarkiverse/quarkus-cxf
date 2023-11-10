package io.quarkiverse.cxf.deployment.logging;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

/**
 * The simplest Hello service.
 */
@WebService(targetNamespace = "http://deployment.logging.features.cxf.quarkiverse.io/")
public interface HelloService {

    @WebMethod
    String hello(String text);

}
