package io.quarkiverse.cxf.client.it;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.eap.quickstarts.wscalculator.basicauthcalculator.BasicAuthCalculatorService;

import io.quarkiverse.cxf.annotation.CXFClient;

@Path("/cxf/client/auth")
public class BasicAuthClientResource {
    @Inject
    @CXFClient("myBasicAuthCalculator") // name used in application.properties
    BasicAuthCalculatorService myBasicAuthCalculator;

    @Inject
    @CXFClient("myBasicAuthAnonymousCalculator") // name used in application.properties
    BasicAuthCalculatorService myBasicAuthAnonymousCalculator;

    @GET
    @Path("/basic/{client}/securedAdd")
    @Produces(MediaType.TEXT_PLAIN)
    public Response secureAdd(@PathParam("client") String client, @QueryParam("a") int a, @QueryParam("b") int b)
            throws IOException {
        try {
            return Response.ok(getClient(client).securedAdd(a, b)).build();
        } catch (Exception e) {
            try (StringWriter stackTrace = new StringWriter(); PrintWriter out = new PrintWriter(stackTrace)) {
                e.printStackTrace(out);
                return Response.serverError().entity(stackTrace.toString()).build();
            }
        }
    }

    private BasicAuthCalculatorService getClient(String client) {
        switch (client) {
            case "myBasicAuthCalculator":
                return myBasicAuthCalculator;
            case "myBasicAuthAnonymousCalculator":
                return myBasicAuthAnonymousCalculator;
            default:
                throw new IllegalStateException("Unexpected client key " + client);
        }
    }

}
