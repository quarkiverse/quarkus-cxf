package io.quarkiverse.cxf.it.ws.rm.server;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import jakarta.xml.ws.soap.Addressing;

@WebService(portName = "WsrmHelloServicePort", serviceName = "WsrmHelloService", targetNamespace = "https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test/ws-rm")
//@Features(classes = { RMFeature.class })
@Addressing(required = true)
public class WsrmHelloServiceImpl implements WsrmHelloService {

    private int invocationCounter = 0;

    @WebMethod
    @Override
    public String hello(String name) {
        invocationCounter++;
        return "WS-ReliableMessaging Hello " + name + "! counter: " + invocationCounter;
    }

}
