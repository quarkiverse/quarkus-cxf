package io.quarkiverse.cxf.it.ws.addressing.server.anonymous;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkiverse.cxf.it.HelloService;

@Path("/ws-addressing-client")
public class AddressingClientResource {

    @CXFClient("addressingSoapHeadersSent")
    HelloService client;

    @POST
    @Path("/call-addressing-headers-enforcer")
    public String callListHeadersClient(String person) {
        return client.hello(person);
    }

}
