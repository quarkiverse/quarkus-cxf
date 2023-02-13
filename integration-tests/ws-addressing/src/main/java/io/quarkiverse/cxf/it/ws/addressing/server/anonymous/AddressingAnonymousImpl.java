package io.quarkiverse.cxf.it.ws.addressing.server.anonymous;

import jakarta.jws.WebParam;
import jakarta.jws.WebService;
import jakarta.xml.ws.BindingType;
import jakarta.xml.ws.soap.Addressing;
import jakarta.xml.ws.soap.SOAPBinding;

@WebService(serviceName = "AddressingAnonymousImpl")
@BindingType(SOAPBinding.SOAP12HTTP_BINDING)
@Addressing(required = true)
public class AddressingAnonymousImpl {

    public String reply(@WebParam(name = "text") String text) {
        return "Hello " + text;
    }

}
