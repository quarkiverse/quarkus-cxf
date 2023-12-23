package io.quarkiverse.cxf.it.ws.addressing.server.decoupled;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;

@WebService(name = "WsAddressingService", serviceName = "WsAddressingService", endpointInterface = "io.quarkiverse.cxf.it.ws.addressing.server.decoupled.WsAddressingService")
public class WsAddressingImpl implements WsAddressingService {

    @WebMethod
    @Override
    public String echo(String message) {
        return message + " from WsAddressingService";
    }

}
