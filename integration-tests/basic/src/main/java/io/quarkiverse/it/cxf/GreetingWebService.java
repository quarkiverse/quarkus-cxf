package io.quarkiverse.it.cxf;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.RequestWrapper;

@WebService
public interface GreetingWebService {

    @WebMethod
    String reply(@WebParam(name = "text") String text);

    @WebMethod
    @RequestWrapper(localName = "Ping", targetNamespace = "http://cxf.it.quarkiverse.io/", className = "io.quarkiverse.it.cxf.Ping")
    String ping(@WebParam(name = "text") String text) throws GreetingException;

}
