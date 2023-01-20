package io.quarkiverse.cxf.features.logging.deployment;

import javax.jws.WebMethod;
import javax.jws.WebService;

/**
 * The simplest Hello service.
 */
@WebService
public interface HelloService {

    @WebMethod
    String hello(String text);

}
