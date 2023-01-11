package io.quarkiverse.cxf.it.server;

import javax.inject.Inject;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.BindingType;

@WebService(endpointInterface = "io.quarkiverse.cxf.it.server.GreetingWebService", serviceName = "GreetingWebService")
@BindingType(javax.xml.ws.soap.SOAPBinding.SOAP12HTTP_BINDING)
public class GreetingWebServiceImpl implements GreetingWebService {

    @Inject
    HelloBean helloResource;

    @Override
    public String reply(@WebParam(name = "text") String text) {
        return helloResource.getHello() + text;
    }

}
