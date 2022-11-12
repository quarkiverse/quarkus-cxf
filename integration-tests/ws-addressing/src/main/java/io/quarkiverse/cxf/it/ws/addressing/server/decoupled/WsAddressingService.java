package io.quarkiverse.cxf.it.ws.addressing.server.decoupled;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.xml.ws.soap.Addressing;

@WebService
@Addressing(required = true)
public interface WsAddressingService {

    @WebMethod
    public String echo(String message);
}
