package io.quarkiverse.cxf.it.ws.rm.server;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

import org.apache.cxf.feature.Features;

@WebService(portName = "WsrmHelloServicePort", serviceName = "WsrmHelloService", targetNamespace = "https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test/ws-rm", endpointInterface = "io.quarkiverse.cxf.it.ws.rm.server.WsrmHelloService")
@Features(classes = { RMStoreFeature.class })
public class WsrmHelloServiceImpl implements WsrmHelloService {

    @WebMethod
    @Override
    public String sayHello(String name) {
        return "WS-ReliableMessaging Hello " + name + "! seqSize: " + RMStoreCheckInterceptor.seqSize;
    }

}
