package io.quarkiverse.cxf.it.server;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

/**
 * A Web service always throwing an exception.
 */
@WebService(serviceName = "FaultyHelloService", portName = "FaultyHelloServicePort")
public class FaultyHelloServiceImpl implements FaultyHelloService {

    @WebMethod
    @Override
    public String faultyHello(String text) throws GreetingException {
        throw new GreetingException("foo", "bar");
    }

}
