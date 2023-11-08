package io.quarkiverse.cxf.it.ws.rm.server;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

@WebService(serviceName = "WsrmHelloService", targetNamespace = "https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test/ws-rm")
public class WsrmHelloServiceImpl implements WsrmHelloService {

    static int invocationCounter = 0;

    @WebMethod
    @Override
    public String hello(String name) {
        invocationCounter++;
        return "WS-ReliableMessaging Hello " + name + "! counter: " + invocationCounter;
    }

}
