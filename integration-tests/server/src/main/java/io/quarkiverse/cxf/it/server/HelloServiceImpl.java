package io.quarkiverse.cxf.it.server;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

/**
 * The simplest Hello service implementation.
 */
@WebService(serviceName = "HelloService", portName = "HelloServicePort")
public class HelloServiceImpl implements HelloService {

    @WebMethod
    @Override
    public String hello(String text) {
        return "Hello " + text + "!";
    }

}
