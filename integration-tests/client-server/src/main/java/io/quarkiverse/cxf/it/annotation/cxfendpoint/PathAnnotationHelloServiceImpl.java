package io.quarkiverse.cxf.it.annotation.cxfendpoint;

import jakarta.jws.WebService;

import io.quarkiverse.cxf.annotation.CXFEndpoint;
import io.quarkiverse.cxf.it.HelloService;

@CXFEndpoint("/path-annotation") // <1>
@WebService(serviceName = "HelloService", targetNamespace = HelloService.NS)
public class PathAnnotationHelloServiceImpl implements HelloService {
    @Override
    public String hello(String person) {
        return "Hello " + person + " from PathAnnotationHelloServiceImpl!";
    }
}
