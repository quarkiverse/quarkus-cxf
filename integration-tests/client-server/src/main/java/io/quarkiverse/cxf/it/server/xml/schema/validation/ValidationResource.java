package io.quarkiverse.cxf.it.server.xml.schema.validation;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkiverse.cxf.it.server.xml.schema.validation.model.CalculatorService;

@Path("/client-server/validation")
public class ValidationResource {

    @CXFClient("application-properties-schema-validated-calculator")
    CalculatorService calculatorService;

    @GET
    @Path("/addCheckResult")
    @Produces(MediaType.TEXT_PLAIN)
    public Response addCheckResult(@QueryParam("a") int a, @QueryParam("b") int b) {
        try {
            return Response.ok(calculatorService.addCheckResult(a, b)).build();
        } catch (Exception e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/addCheckParameters")
    @Produces(MediaType.TEXT_PLAIN)
    public Response addCheckParameters(@QueryParam("a") int a, @QueryParam("b") int b) {
        try {
            return Response.ok(calculatorService.addCheckParameters(a, b)).build();
        } catch (Exception e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

}
