package io.quarkiverse.cxf.it;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

@WebService(serviceName = "HelloService", targetNamespace = HelloService.NS)
public interface HelloService {
    public static final String NS = "https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test";

    @WebMethod
    public String hello(String person);
}
