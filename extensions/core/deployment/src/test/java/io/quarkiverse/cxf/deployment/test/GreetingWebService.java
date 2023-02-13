package io.quarkiverse.cxf.deployment.test;

import jakarta.jws.WebMethod;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;

@WebService
public interface GreetingWebService {

    @WebMethod
    @WebResult(name = "hello", targetNamespace = "http://test.deployment.cxf.quarkiverse.io/", partName = "parameters")
    String hello();

    @WebMethod
    @WebResult(name = "ping", targetNamespace = "http://test.deployment.cxf.quarkiverse.io/", partName = "parameters")
    void ping();

}
