package io.quarkiverse.cxf.it.auth.oidc;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebService;

@WebService(serviceName = "HelloService", targetNamespace = HelloService.NS)
public interface HelloService {

    String NS = "http://oidc.auth.it.cxf.quarkiverse.io/";

    @WebMethod
    String hello(@WebParam(name = "name") String name);

    @WebMethod
    String hallo(@WebParam(name = "name") String name);

    @WebMethod
    String ciao(@WebParam(name = "name") String name);

    @WebMethod
    String hoi(@WebParam(name = "name") String name);
}
