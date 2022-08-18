package io.quarkiverse.cxf.it.logging;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.eap.quickstarts.wscalculator.calculator.CalculatorService;

import io.quarkiverse.cxf.annotation.CXFClient;

@Path("/cxf/logging")
public class CxfLoggingResource {

    @Inject
    @CXFClient("logging-client") // name used in application.properties
    CalculatorService calculator;

    @GET
    @Path("/calculator/multiply")
    @Produces(MediaType.TEXT_PLAIN)
    public Response multiplyDefault(@QueryParam("a") int a, @QueryParam("b") int b) {
        try {
            return Response.ok(calculator.multiply(a, b)).build();
        } catch (Exception e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

}
