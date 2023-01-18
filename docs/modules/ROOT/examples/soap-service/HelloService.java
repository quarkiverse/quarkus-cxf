package io.quarkiverse.cxf.it.server;

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
