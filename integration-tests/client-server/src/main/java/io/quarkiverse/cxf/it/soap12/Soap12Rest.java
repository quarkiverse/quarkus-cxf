package io.quarkiverse.cxf.it.soap12;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkiverse.cxf.it.HelloService;

@Path("/Soap12Rest")
public class Soap12Rest {

    @CXFClient("soap12")
    HelloService soap12;

    @CXFClient("contentType")
    HelloService contentType;

    @CXFClient("contentTypeSoap12")
    HelloService contentTypeSoap12;

    HelloService getClient(String clientName) {
        switch (clientName) {
            case "soap12": {
                return soap12;
            }
            case "contentType": {
                return contentType;
            }
            case "contentTypeSoap12": {
                return contentTypeSoap12;
            }
            default:
                throw new IllegalArgumentException("Unexpected client name: " + clientName);
        }
    }

    @Path("/sync/{client}")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String hello(@PathParam("client") String client, String body) {
        return getClient(client).hello(body);
    }

}
