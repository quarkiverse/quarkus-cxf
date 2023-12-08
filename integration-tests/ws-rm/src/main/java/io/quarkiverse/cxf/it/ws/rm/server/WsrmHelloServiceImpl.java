package io.quarkiverse.cxf.it.ws.rm.server;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

import org.apache.cxf.feature.Features;
import org.apache.cxf.ws.rm.feature.RMFeature;

@WebService(portName = "WsrmHelloServicePort", serviceName = "WsrmHelloService", targetNamespace = "https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test/ws-rm", endpointInterface = "io.quarkiverse.cxf.it.ws.rm.server.WsrmHelloService")
@Features(classes = { RMFeature.class })
public class WsrmHelloServiceImpl implements WsrmHelloService {

    static int invocationCounter = 0;

    @WebMethod
    @Override
    public String sayHello(String name) {
        invocationCounter++;
        return "WS-ReliableMessaging Hello " + name + "! counter: " + invocationCounter;
    }

}
