package io.quarkiverse.cxf.it.security.policy;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

/**
 * The simplest Hello service.
 */
@WebService(name = "HelloService", serviceName = "HelloService")
public interface HelloIpService {

    @WebMethod
    String hello(String text);

}
