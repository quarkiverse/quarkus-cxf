package io.quarkiverse.cxf.it.security.policy;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

/**
 * The simplest Hello service.
 */
@WebService(serviceName = "HelloService")
public interface HelloService extends AbstractHelloService {

    @WebMethod
    @Override
    String hello(String text);

}
