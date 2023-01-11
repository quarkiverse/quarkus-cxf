package io.quarkiverse.cxf.it.server;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.xml.ws.BindingType;

/**
 * A Web service always throwing an exception.
 */
@WebService(endpointInterface = "io.quarkiverse.cxf.it.server.FaultyHelloService", serviceName = "FaultyHelloService")
@BindingType(javax.xml.ws.soap.SOAPBinding.SOAP12HTTP_BINDING)
public class FaultyHelloServiceImpl implements FaultyHelloService {

    @WebMethod
    @Override
    public String hello(String text) throws GreetingException {
        throw new GreetingException("foo", "bar");
    }

}
