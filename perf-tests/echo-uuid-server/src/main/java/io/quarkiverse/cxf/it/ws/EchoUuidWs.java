package io.quarkiverse.cxf.it.ws;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebService;

@WebService(targetNamespace = "http://l2x6.org/echo-uuid-ws/", name = "EchoUuidWs")
public interface EchoUuidWs {
    @WebMethod
    public String echoUuid(@WebParam(name = "uuid") String uuid);
}
