package io.quarkiverse.cxf.it.server;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebService;

@WebService
public interface GreetingWebService {

    @WebMethod
    String reply(@WebParam(name = "text") String text);

}
