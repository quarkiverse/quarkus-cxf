package io.quarkiverse.cxf.it.resteasy;

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
