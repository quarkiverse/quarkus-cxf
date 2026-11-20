package io.quarkiverse.cxf.it.nsprefix;

import jakarta.jws.WebService;

import io.quarkiverse.cxf.it.HelloService;

@WebService(serviceName = "HelloService", targetNamespace = HelloService.NS)
public class NamespacePrefixHelloServiceImpl implements HelloService {

    @Override
    public String hello(String person) {
        return "Hello " + person + "!";
    }

}
