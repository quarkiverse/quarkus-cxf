package io.quarkiverse.cxf.it.extensors;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkiverse.cxf.it.server.extensors.model.ExtensorsService;

@Path("/ExtensorsRest")
public class ExtensorsRest {

    @CXFClient("extensors")
    ExtensorsService extensors;

    @Path("/hello/{name}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello(@PathParam("name") String name) {
        return extensors.hello(name);
    }

}
