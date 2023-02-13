package io.quarkiverse.cxf.it.server;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import jakarta.xml.ws.BindingType;

/**
 * A Web service always throwing an exception.
 */
@WebService(endpointInterface = "io.quarkiverse.cxf.it.server.FaultyHelloService", serviceName = "FaultyHelloService")
@BindingType(jakarta.xml.ws.soap.SOAPBinding.SOAP12HTTP_BINDING)
public class FaultyHelloServiceImpl implements FaultyHelloService {

    @WebMethod
    @Override
    public String hello(String text) throws GreetingException {
        throw new GreetingException("foo", "bar");
    }

}
