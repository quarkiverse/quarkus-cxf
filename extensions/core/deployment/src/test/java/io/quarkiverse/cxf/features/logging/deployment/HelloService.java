package io.quarkiverse.cxf.features.logging.deployment;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

/**
 * The simplest Hello service.
 */
@WebService
public interface HelloService {

    @WebMethod
    String hello(String text);

}
