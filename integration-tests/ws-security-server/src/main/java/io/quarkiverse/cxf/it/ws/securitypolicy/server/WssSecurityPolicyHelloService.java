package io.quarkiverse.cxf.it.ws.securitypolicy.server;

import javax.jws.WebMethod;
import javax.jws.WebService;

@WebService(targetNamespace = "https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/ws-securitypolicy")
public interface WssSecurityPolicyHelloService {

    @WebMethod
    String sayHello(String name);
}
