package io.quarkiverse.it.cxf;

import javax.inject.Inject;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.BindingType;

@WebService(endpointInterface = "io.quarkiverse.it.cxf.GreetingWebService", serviceName = "GreetingWebService")
@BindingType(javax.xml.ws.soap.SOAPBinding.SOAP12HTTP_BINDING)
public class GreetingWebServiceImpl implements GreetingWebService {

    @Inject
    public HelloBean helloResource;

    @Override
    public String reply(@WebParam(name = "text") String text) {
        return helloResource.getHello() + text;
    }

    @Override
    public String ping(@WebParam(name = "text") String text) throws GreetingException {
        if (text.equals("error")) {
            throw new GreetingException("foo", "bar");
        }
        return helloResource.getHello() + text;
    }

}
