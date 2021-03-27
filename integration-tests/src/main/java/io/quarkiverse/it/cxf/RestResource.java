package io.quarkiverse.it.cxf;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.quarkiverse.cxf.annotation.CXFClient;

@Path("/rest")
public class RestResource {
    @Inject
    @CXFClient
    public GreetingClientWebService greetingWS;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String get() {
        return greetingWS.reply("foo");
    }
}
