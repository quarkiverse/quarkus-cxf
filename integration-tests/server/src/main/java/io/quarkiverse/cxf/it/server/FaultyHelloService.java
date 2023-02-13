package io.quarkiverse.cxf.it.server;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

@WebService
public interface FaultyHelloService {

    @WebMethod
    String hello(String text) throws GreetingException;

}
