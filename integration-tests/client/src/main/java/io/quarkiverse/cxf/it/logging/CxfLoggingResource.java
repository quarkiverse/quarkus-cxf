package io.quarkiverse.cxf.it.logging;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.eap.quickstarts.wscalculator.calculator.CalculatorService;

import io.quarkiverse.cxf.annotation.CXFClient;

@Path("/cxf/logging")
public class CxfLoggingResource {

    @Inject
    @CXFClient("beanConfiguredCalculator") // name used in application.properties
    CalculatorService beanConfiguredCalculator;

    @Inject
    @CXFClient("propertiesConfiguredCalculator") // name used in application.properties
    CalculatorService propertiesConfiguredCalculator;

    @GET
    @Path("/beanConfiguredCalculator/multiply")
    @Produces(MediaType.TEXT_PLAIN)
    public Response multiply(@QueryParam("a") int a, @QueryParam("b") int b) {
        try {
            return Response.ok(beanConfiguredCalculator.multiply(a, b)).build();
        } catch (Exception e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/propertiesConfiguredCalculator/add")
    @Produces(MediaType.TEXT_PLAIN)
    public Response add(@QueryParam("a") int a, @QueryParam("b") int b) {
        try {
            return Response.ok(propertiesConfiguredCalculator.add(a, b)).build();
        } catch (Exception e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

}
