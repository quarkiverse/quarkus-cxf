package io.quarkiverse.cxf.it.auth.basic;

import jakarta.annotation.security.RolesAllowed;
import jakarta.jws.WebService;

import io.quarkiverse.cxf.it.HelloService;

@WebService(serviceName = "HelloService", targetNamespace = HelloService.NS)
@RolesAllowed("app-user")
public class BasicAuthHelloServiceImpl implements HelloService {
    @Override
    public String hello(String person) {
        return "Hello " + person + "!";
    }
}
