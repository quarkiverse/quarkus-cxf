package io.quarkiverse.cxf.it.annotation.cxfendpoint;

import jakarta.inject.Inject;
import jakarta.jws.WebService;

import io.quarkiverse.cxf.annotation.CXFEndpoint;
import io.quarkiverse.cxf.it.HelloService;

@CXFEndpoint("/path-annotation-with-bean")
@WebService(serviceName = "HelloService", targetNamespace = HelloService.NS)
public class PathAnnotationHelloServiceWithBean implements HelloService {

    @Inject
    HelloBean helloBean;

    @Override
    public String hello(String person) {
        return helloBean.hello(person);
    }
}
