package io.quarkiverse.cxf.it.ws.addressing.server.anonymous;

import jakarta.jws.WebService;
import jakarta.xml.ws.BindingType;
import jakarta.xml.ws.soap.Addressing;
import jakarta.xml.ws.soap.SOAPBinding;

import io.quarkiverse.cxf.it.HelloService;

@WebService(serviceName = "HelloService", targetNamespace = HelloService.NS)
@BindingType(SOAPBinding.SOAP12HTTP_BINDING)
@Addressing(required = true)
public class AddressingAnonymousImpl implements HelloService {

    @Override
    public String hello(String text) {
        return "Hello " + text + " from AddressingAnonymousImpl";
    }

}
