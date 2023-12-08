package io.quarkiverse.cxf.it.ws.rm.server;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import jakarta.xml.ws.soap.Addressing;

@WebService(targetNamespace = "https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test/ws-rm")
@Addressing(required = true)
//@InInterceptors(interceptors = { "io.quarkiverse.cxf.it.ws.rm.server.RMStoreCheckInterceptor" })
public interface WsrmHelloService {
    @WebMethod
    String sayHello(String name);
}
