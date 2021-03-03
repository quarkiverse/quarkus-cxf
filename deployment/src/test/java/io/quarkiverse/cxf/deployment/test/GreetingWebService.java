package io.quarkiverse.cxf.deployment.test;

import javax.jws.WebMethod;
import javax.jws.WebResult;
import javax.jws.WebService;

@WebService
public interface GreetingWebService {

    @WebMethod
    @WebResult(name = "hello", targetNamespace = "http://test.deployment.cxf.quarkiverse.io/", partName = "parameters")
    String hello();

    @WebMethod
    @WebResult(name = "ping", targetNamespace = "http://test.deployment.cxf.quarkiverse.io/", partName = "parameters")
    void ping();

}
