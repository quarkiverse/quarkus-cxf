package io.quarkiverse.cxf.it.ws.rm.server;

import javax.jws.WebMethod;
import javax.jws.WebService;

import org.apache.cxf.feature.Features;

@WebService(portName = "WsrmHelloServicePort", serviceName = "WsrmHelloService", targetNamespace = "https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test/ws-rm", endpointInterface = "io.quarkiverse.cxf.it.ws.rm.server.WsrmHelloService")
@Features(classes = { RMStoreFeature.class })
public class WsrmHelloServiceImpl implements WsrmHelloService {

    @WebMethod
    public String sayHello() {
        return "WS-ReliableMessaging Hello World! seqSize: " + RMStoreCheckInterceptor.seqSize;
    }

}
