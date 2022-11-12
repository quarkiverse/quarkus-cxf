package io.quarkiverse.cxf.it.ws.addressing.server.anonymous;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.BindingType;
import javax.xml.ws.soap.Addressing;

@WebService(serviceName = "AddressingAnonymousImpl")
@BindingType(javax.xml.ws.soap.SOAPBinding.SOAP12HTTP_BINDING)
@Addressing(required = true)
public class AddressingAnonymousImpl {

    public String reply(@WebParam(name = "text") String text) {
        return "Hello " + text;
    }

}
