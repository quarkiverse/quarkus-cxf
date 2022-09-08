package io.quarkiverse.cxf.it.server;

import javax.inject.Inject;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.BindingType;
import javax.xml.ws.soap.Addressing;

@WebService(serviceName = "GreetingWebServiceAddressingImpl")
@BindingType(javax.xml.ws.soap.SOAPBinding.SOAP12HTTP_BINDING)
@Addressing(required = true)
public class GreetingWebServiceAddressingImpl {

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
