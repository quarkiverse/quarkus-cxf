package io.quarkiverse.cxf.it.ws.trust.server;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

@WebService(portName = "TrustHelloServicePort", serviceName = "TrustHelloService", wsdlLocation = "TrustHelloService.wsdl", targetNamespace = "https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test/ws-trust", endpointInterface = "io.quarkiverse.cxf.it.ws.trust.server.TrustHelloService")
public class TrustHelloServiceImpl implements TrustHelloService {
    @WebMethod
    @Override
    public String sayHello() {
        return "WS-Trust Hello World!";
    }
}
