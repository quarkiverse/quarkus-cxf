package io.quarkiverse.cxf.it.server;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

@WebService(name = "FaultyHelloService", serviceName = "FaultyHelloService")
public interface FaultyHelloService {

    @WebMethod
    String faultyHello(String text) throws GreetingException;

}
