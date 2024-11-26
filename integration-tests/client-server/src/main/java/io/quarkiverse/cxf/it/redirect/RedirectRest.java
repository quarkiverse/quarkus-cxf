package io.quarkiverse.cxf.it.redirect;

import java.net.URI;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkiverse.cxf.it.large.slow.generated.LargeSlowService;
import io.smallrye.mutiny.Uni;

@Path("/RedirectRest")
public class RedirectRest {

    @CXFClient("singleRedirect")
    LargeSlowService singleRedirect;

    @CXFClient("doubleRedirect")
    LargeSlowService doubleRedirect;

    @CXFClient("tripleRedirect")
    LargeSlowService tripleRedirect;

    @CXFClient("noAutoRedirect")
    LargeSlowService noAutoRedirect;

    @CXFClient("doubleRedirectMaxRetransmits1")
    LargeSlowService doubleRedirectMaxRetransmits1;

    @CXFClient("doubleRedirectMaxRetransmits2")
    LargeSlowService doubleRedirectMaxRetransmits2;

    @CXFClient("loop")
    LargeSlowService loop;

    LargeSlowService getClient(String clientName) {
        switch (clientName) {
            case "singleRedirect": {
                return singleRedirect;
            }
            case "doubleRedirect": {
                return doubleRedirect;
            }
            case "tripleRedirect": {
                return tripleRedirect;
            }
            case "noAutoRedirect": {
                return noAutoRedirect;
            }
            case "doubleRedirectMaxRetransmits1": {
                return doubleRedirectMaxRetransmits1;
            }
            case "doubleRedirectMaxRetransmits2": {
                return doubleRedirectMaxRetransmits2;
            }
            case "loop": {
                return loop;
            }
            default:
                throw new IllegalArgumentException("Unexpected client name: " + clientName);
        }
    }

    @Path("/singleRedirect")
    @POST
    public Response singleRedirect(String body) {
        // Log.infof("/RedirectRest/singleRedirect Received payload %s", body);
        // Relative URI: return Response.status(302).header("Location", "/soap/largeSlow").build();
        // RestEASY will convert this to an absolute URI
        return Response.temporaryRedirect(URI.create("/soap/largeSlow")).build();
    }

    @Path("/doubleRedirect")
    @POST
    public Response doubleRedirect(String body) {
        // Log.infof("/RedirectRest/doubleRedirect Received payload %s", body);
        return Response.status(302).header("Location", "/RedirectRest/singleRedirect").build();
    }

    @Path("/tripleRedirect")
    @POST
    public Response tripleRedirect() {
        return Response.status(302).header("Location", "/RedirectRest/doubleRedirect").build();
    }

    @Path("/loop1")
    @POST
    public Response loop1() {
        return Response.temporaryRedirect(URI.create("/RedirectRest/loop2")).build();
    }

    @Path("/loop2")
    @POST
    public Response loop2() {
        // Log.infof("/RedirectRest/singleRedirect Received payload %s", body);
        // Relative URI: return Response.status(302).header("Location", "/soap/largeSlow").build();
        // RestEASY will convert this to an absolute URI
        return Response.temporaryRedirect(URI.create("/RedirectRest/loop1")).build();
    }

    @Path("/async/{client}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> async(@PathParam("client") String client, @QueryParam("sizeBytes") int sizeBytes,
            @QueryParam("delayMs") int delayMs) {
        return Uni.createFrom()
                .future(getClient(client).largeSlowAsync(sizeBytes, delayMs))
                .map(addResponse -> addResponse.getReturn().getPayload());
    }

    @Path("/sync/{client}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String largeHelloSync(@PathParam("client") String client, @QueryParam("sizeBytes") int sizeBytes,
            @QueryParam("delayMs") int delayMs) {
        return getClient(client).largeSlow(sizeBytes, delayMs).getPayload();
    }

}
