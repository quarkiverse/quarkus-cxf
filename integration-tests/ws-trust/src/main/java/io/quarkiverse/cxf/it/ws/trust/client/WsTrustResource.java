package io.quarkiverse.cxf.it.ws.trust.client;

import java.io.PrintWriter;
import java.io.StringWriter;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkiverse.cxf.it.ws.trust.server.TrustHelloService;

@Path("/ws-trust")
public class WsTrustResource {

    @Inject
    @CXFClient("hello-ws-trust")
    TrustHelloService hello;

    @Inject
    @CXFClient("hello-ws-trust-bean")
    TrustHelloService helloBean;

    @POST
    @Path("/hello/{client}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response hello(@PathParam("client") String clientKey, String body) {
        final TrustHelloService client = switch (clientKey) {
            case "hello-ws-trust": {
                yield hello;
            }
            case "hello-ws-trust-bean": {
                yield helloBean;
            }
            default:
                throw new IllegalArgumentException("Unexpected client key: " + clientKey);
        };
        try {
            return Response.ok(client.hello(body)).build();
        } catch (Exception e) {
            final StringWriter w = new StringWriter();
            final PrintWriter pw = new PrintWriter(w);
            e.printStackTrace(pw);
            return Response.status(500).entity(w.toString()).build();
        }
    }

}
