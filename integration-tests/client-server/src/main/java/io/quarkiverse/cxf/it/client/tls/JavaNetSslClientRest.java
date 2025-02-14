package io.quarkiverse.cxf.it.client.tls;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkiverse.cxf.it.HelloService;
import io.quarkus.logging.Log;

@Path("/JavaNetSslClient")
public class JavaNetSslClientRest {

    @CXFClient("javaNetSslClient")
    HelloService javaNetSslClient;

    HelloService getClient(String clientName) {
        switch (clientName) {
            case "javaNetSslClient": {
                return javaNetSslClient;
            }
            default:
                throw new IllegalArgumentException("Unexpected client name: " + clientName);
        }
    }

    @Path("/sync/{client}")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String hello(@PathParam("client") String client, String body) {
        Log.infof("javax.net.ssl.trustStore = %s", System.getProperty("javax.net.ssl.trustStore"));
        return getClient(client).hello(body);
    }

}
