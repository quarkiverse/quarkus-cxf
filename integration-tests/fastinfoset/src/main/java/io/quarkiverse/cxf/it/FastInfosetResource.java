package io.quarkiverse.cxf.it;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.quarkiverse.cxf.annotation.CXFClient;

@Path("/fastinfoset")
public class FastInfosetResource {

    @CXFClient("gzip")
    GzipHelloService gzip;

    @CXFClient("fastinfoset")
    FastInfosetHelloService fastinfoset;

    @POST
    @Path("/{client}/hello")
    @Produces(MediaType.TEXT_PLAIN)
    public Response hello(String body, @PathParam("client") String client) {
        try {
            switch (client) {
                case "gzip": {
                    return Response.ok(gzip.hello(body)).build();
                }
                case "fastinfoset": {
                    return Response.ok(fastinfoset.hello(body)).build();
                }
                default:
                    throw new IllegalArgumentException("Unexpected client: " + client);
            }
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
