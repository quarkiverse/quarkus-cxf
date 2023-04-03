package io.quarkiverse.cxf.it.resteasy;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/rest")
public class RestEasyResource {
    @GET
    @Path("/hello/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    public String resource(@PathParam("name") String name) {
        return "Hello from RESTEasy " + name + "!";
    }
}
