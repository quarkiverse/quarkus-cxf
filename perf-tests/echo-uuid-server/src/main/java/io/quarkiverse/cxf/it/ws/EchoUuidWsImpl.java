package io.quarkiverse.cxf.it.ws;

import jakarta.jws.WebService;

import io.quarkiverse.cxf.annotation.CXFEndpoint;

@CXFEndpoint("echo-uuid-ws/soap-1.1")
@WebService(serviceName = "EchoUuidWs", targetNamespace = "http://l2x6.org/echo-uuid-ws/")
public class EchoUuidWsImpl implements EchoUuidWs {

    @Override
    public String echoUuid(String uuid) {
        return uuid;
    }

}
