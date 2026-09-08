package io.quarkiverse.cxf.it.mutable.jar;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import jakarta.xml.ws.soap.Addressing;

/**
 * The simplest Hello service.
 */
@WebService(name = "HelloService", serviceName = "HelloService")
@Addressing(required = true)
public interface HelloService {

    @WebMethod
    String hello(String text);

}
