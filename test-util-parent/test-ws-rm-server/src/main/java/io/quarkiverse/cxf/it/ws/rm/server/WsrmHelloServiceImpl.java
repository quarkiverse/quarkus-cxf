package io.quarkiverse.cxf.it.ws.rm.server;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

@WebService(serviceName = "WsrmHelloService", targetNamespace = "https://quarkiverse.github.io/quarkiverse-docs/quarkus-cxf/test/ws-rm")
public class WsrmHelloServiceImpl implements WsrmHelloService {

    private AtomicInteger invocationCounter = new AtomicInteger(0);

    @WebMethod
    @Override
    public String hello(String name) {
        return "WS-ReliableMessaging Hello " + name + "! counter: " + invocationCounter.incrementAndGet();
    }

}
