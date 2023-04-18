package io.quarkiverse.cxf.it.server;

import javax.jws.WebMethod;
import javax.jws.WebService;

@WebService(name = "FaultyHelloService", serviceName = "FaultyHelloService")
public interface FaultyHelloService {

    @WebMethod
    String faultyHello(String text) throws GreetingException;

}
