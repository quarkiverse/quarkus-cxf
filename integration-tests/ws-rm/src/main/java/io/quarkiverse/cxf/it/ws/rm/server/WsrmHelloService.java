package io.quarkiverse.cxf.it.ws.rm.server;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import jakarta.xml.ws.soap.Addressing;

@WebService(serviceName = "WsrmHelloService", targetNamespace = "https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test/ws-rm")
@Addressing(required = true)
public interface WsrmHelloService {
    @WebMethod
    String hello(String name);
}
