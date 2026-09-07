package io.quarkiverse.cxf.deployment.test;

import java.util.Set;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import jakarta.xml.ws.RequestWrapper;

@WebService
public interface FruitWebService {

    @WebMethod
    @WebResult(name = "countFruitsResponse", targetNamespace = "http://test.deployment.cxf.quarkiverse.io/", partName = "parameters")
    int count();

    @WebMethod
    @RequestWrapper(localName = "add", targetNamespace = "http://test.deployment.cxf.quarkiverse.io/", className = "io.quarkiverse.cxf.deployment.test.Add")
    Set<Fruit> add(@WebParam(name = "fruit") Fruit fruit);

    @WebMethod
    @RequestWrapper(localName = "delete", targetNamespace = "http://test.deployment.cxf.quarkiverse.io/", className = "io.quarkiverse.cxf.deployment.test.Delete")
    void delete(@WebParam(name = "deletedfruit") Fruit fruit);

    @WebMethod
    String getDescriptionByName(@WebParam(name = "name") String name);

}
