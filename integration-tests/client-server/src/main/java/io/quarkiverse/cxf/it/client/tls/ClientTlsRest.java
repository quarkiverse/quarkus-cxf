package io.quarkiverse.cxf.it.client.tls;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkiverse.cxf.it.HelloService;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/ClientTlsRest")
public class ClientTlsRest {

    @CXFClient("vertxClient")
    HelloService vertxClient;

    @CXFClient("urlConnectionClient")
    HelloService urlConnectionClient;

    @CXFClient("httpClient")
    HelloService httpClient;

    HelloService getClient(String clientName) {
        switch (clientName) {
            case "vertxClient": {
                return vertxClient;
            }
            case "urlConnectionClient": {
                return urlConnectionClient;
            }
            case "httpClient": {
                return httpClient;
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
