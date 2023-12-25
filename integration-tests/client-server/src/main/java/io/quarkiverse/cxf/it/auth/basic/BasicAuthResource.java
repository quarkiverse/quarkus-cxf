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

    @CXFClient("basicAuthAnonymous")
    HelloService basicAuthAnonymous;

    @CXFClient("basicAuthBadUser")
    HelloService basicAuthBadUser;

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
            case "basicAuthBadUser": {
                yield basicAuthBadUser;
            }
            case "basicAuthAnonymous": {
                yield basicAuthAnonymous;
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
            Throwable rootCause = rootCause(e);
            return Response.serverError().entity(rootCause.getMessage()).build();
        }
    }

    private static Throwable rootCause(Exception e) {
        e.printStackTrace();
        Throwable result = e;
        while (result.getCause() != null) {
            result = result.getCause();
        }
        return result;
    }

}
