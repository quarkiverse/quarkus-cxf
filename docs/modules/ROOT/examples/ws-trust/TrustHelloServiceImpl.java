package io.quarkiverse.cxf.it.ws.trust.server;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

//tag::ws-trust-usage.adoc-service[]
@WebService(portName = "TrustHelloServicePort", serviceName = "TrustHelloService", targetNamespace = "https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test/ws-trust", endpointInterface = "io.quarkiverse.cxf.it.ws.trust.server.TrustHelloService")
public class TrustHelloServiceImpl implements TrustHelloService {
    @WebMethod
    @Override
    public String hello(String person) {
        return "Hello " + person + "!";
    }
}
//end::ws-trust-usage.adoc-service[]