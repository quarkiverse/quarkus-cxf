package io.quarkiverse.cxf.it.ws.addressing.server.decoupled;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import jakarta.xml.ws.soap.Addressing;

@WebService
@Addressing(required = true)
public interface WsAddressingService {

    @WebMethod
    public String echo(String message);
}
