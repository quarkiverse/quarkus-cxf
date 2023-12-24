package io.quarkiverse.cxf.it.auth.basic;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkiverse.cxf.it.HelloService;

@Path("/client-server/basic-auth")
public class BasicAuthResource {

    @CXFClient("basicAuth")
    HelloService basicAuth;

    @CXFClient("basicAuthSecureWsdl")
    HelloService basicAuthSecureWsdl;

    @POST
    @Path("/{client}/hello")
    @Produces(MediaType.TEXT_PLAIN)
    public Response hello(String body, @PathParam("client") String client) {
        final HelloService helloService = switch (client) {
            case "basicAuth": {
                yield basicAuth;
            }
            case "basicAuthSecureWsdl": {
                yield basicAuthSecureWsdl;
            }
            default:
                throw new IllegalArgumentException("Unexpected client: " + client);
        };

        try {
            return Response.ok(helloService.hello(body)).build();
        } catch (Exception e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

}
