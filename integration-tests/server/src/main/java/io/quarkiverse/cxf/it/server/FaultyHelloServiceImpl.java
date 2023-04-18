package io.quarkiverse.cxf.it.server;

import javax.jws.WebMethod;
import javax.jws.WebService;

/**
 * A Web service always throwing an exception.
 */
@WebService(serviceName = "FaultyHelloService")
public class FaultyHelloServiceImpl implements FaultyHelloService {

    @WebMethod
    @Override
    public String faultyHello(String text) throws GreetingException {
        throw new GreetingException("foo", "bar");
    }

}
