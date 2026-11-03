package io.quarkiverse.cxf.it.auth.oidc;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.jws.WebService;

import io.quarkus.security.Authenticated;

@WebService(serviceName = "HelloService", targetNamespace = HelloService.NS)
public class HelloServiceImpl implements HelloService {

    @Override
    public String hello(String name) {
        return "Hello " + name + "!";
    }

    @Override
    @PermitAll
    public String hallo(String name) {
        return "Hallo " + name + "!";
    }

    @Override
    @RolesAllowed("italian")
    public String ciao(String name) {
        return "Ciao " + name + "!";
    }

    @Override
    @Authenticated
    public String hoi(String name) {
        return "Hoi " + name + "!";
    }
}
