package io.quarkiverse.it.cxf;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.BindingType;

@WebService(endpointInterface = "io.quarkiverse.it.cxf.GreetingWebService", serviceName = "GreetingWebService")
@BindingType(javax.xml.ws.soap.SOAPBinding.SOAP12HTTP_BINDING)
public class GreetingWebServiceImpl implements GreetingWebService {

    @Inject
    public HelloResource helloResource;

    @PostConstruct
    public void postConstruct() {
        if (helloResource == null) {
            this.helloResource = CDI.current().select(HelloResource.class).get();
        }
    }

    @Override
    public String reply(@WebParam(name = "text") String text) {
        return helloResource.getHello() + text;
    }

    @Override
    public String ping(@WebParam(name = "text") String text) {
        return helloResource.getHello() + text;
    }

}
