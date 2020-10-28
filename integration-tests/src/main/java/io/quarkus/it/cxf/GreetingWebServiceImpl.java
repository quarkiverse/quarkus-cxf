package io.quarkus.it.cxf;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.BindingType;

@WebService(endpointInterface = "io.quarkus.it.cxf.GreetingWebService", serviceName = "GreetingWebService")
@BindingType(javax.xml.ws.soap.SOAPBinding.SOAP12HTTP_BINDING)
public class GreetingWebServiceImpl implements GreetingWebService {

    @Override
    public String reply(@WebParam(name = "text") String text) {
        return "Hello " + text;
    }
}
