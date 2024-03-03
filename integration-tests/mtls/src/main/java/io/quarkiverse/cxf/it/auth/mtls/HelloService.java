package io.quarkiverse.cxf.it.auth.mtls;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

/**
 * The simplest Hello service.
 */
@WebService(serviceName = "HelloService")
public interface HelloService {

    @WebMethod
    String hello(String text);

}
