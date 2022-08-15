package io.quarkiverse.it.cxf;

import javax.inject.Inject;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.BindingType;

/**
 * Greeting web service that does not implement an interface
 */
@WebService(serviceName = "GreetingWebServiceNoIntf")
@BindingType(javax.xml.ws.soap.SOAPBinding.SOAP12HTTP_BINDING)
public class GreetingWebServiceNoIntf {

    @Inject
    HelloBean helloResource;

    public String reply(@WebParam(name = "text") String text) {
        return helloResource.getHello() + text;
    }

    public String ping(@WebParam(name = "text") String text) throws GreetingException {
        if (text.equals("error")) {
            throw new GreetingException("foo", "bar");
        }
        return helloResource.getHello() + text;
    }

}
