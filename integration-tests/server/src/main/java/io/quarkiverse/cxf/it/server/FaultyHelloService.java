package io.quarkiverse.cxf.it.server;

import javax.jws.WebMethod;
import javax.jws.WebService;

@WebService
public interface FaultyHelloService {

    @WebMethod
    String hello(String text) throws GreetingException;

}
