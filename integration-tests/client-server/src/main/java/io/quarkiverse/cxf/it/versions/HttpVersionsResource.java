package io.quarkiverse.cxf.it.versions;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkiverse.cxf.annotation.CXFClient;

@Path("/http-versions")
public class HttpVersionsResource {
    @CXFClient("versionsStrict")
    VersionEcho strict;

    @CXFClient("versionsUpgrade")
    VersionEcho upgrade;

    @CXFClient("versionsLegacy")
    VersionEcho legacy;

    @CXFClient("versionsHttp1")
    VersionEcho http1;

    @POST
    @Path("/{client}")
    @Produces(MediaType.TEXT_PLAIN)
    public String echo(@PathParam("client") String client, String body) {
        final VersionEcho echo = switch (client) {
            case "strict" -> strict;
            case "upgrade" -> upgrade;
            case "legacy" -> legacy;
            case "http1" -> http1;
            default -> throw new IllegalArgumentException(client);
        };
        return echo.echo(body);
    }
}
