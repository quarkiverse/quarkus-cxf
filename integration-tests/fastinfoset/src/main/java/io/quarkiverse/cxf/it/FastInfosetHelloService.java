package io.quarkiverse.cxf.it;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

import org.apache.cxf.annotations.FastInfoset;

@WebService(serviceName = "HelloService", targetNamespace = FastInfosetHelloService.NS)
@FastInfoset(force = true)
public interface FastInfosetHelloService {
    public static final String NS = "https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test";

    @WebMethod
    public String hello(String person);
}
