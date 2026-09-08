package io.quarkiverse.cxf.it.mutable.jar;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

import io.quarkiverse.cxf.annotation.CXFEndpoint;

/**
 * The simplest Hello service implementation.
 */
@WebService(serviceName = "HelloService")
@CXFEndpoint("/hello")
public class HelloServiceImpl implements HelloService {

    @WebMethod
    @Override
    public String hello(String text) {
        return "Hello " + text + "!";
    }

}
