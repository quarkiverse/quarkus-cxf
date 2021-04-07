package io.quarkiverse.it.cxf;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/rest")
public class RestResource {
    @Inject
    @Named
    public GreetingWebService greetingWS;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String get() {
        return greetingWS.reply("foo");
    }
}
