package io.quarkiverse.it.cxf;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

@WebService
public interface GreetingClientWebService {

    @WebMethod
    String reply(@WebParam(name = "text") String text);
}