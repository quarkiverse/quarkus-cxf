package io.quarkiverse.cxf.it.clustering;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.eap.quickstarts.wscalculator.calculator.CalculatorService;

import io.quarkiverse.cxf.annotation.CXFClient;

@Path("/cxf/clustering")
public class CxfClusteringResource {

    @Inject
    @CXFClient("failover-client") // name used in application.properties
    CalculatorService failoverCalculator;

    @Inject
    @CXFClient("load-distributor-client") // name used in application.properties
    CalculatorService loadDistributorCalculator;

    @Inject
    @CXFClient("circuit-breaker-client") // name used in application.properties
    CalculatorService circuitBreakerCalculator;

    @GET
    @Path("/failoverCalculator/multiply")
    @Produces(MediaType.TEXT_PLAIN)
    public Response multiplyFailover(@QueryParam("a") int a, @QueryParam("b") int b) {
        try {
            return Response.ok(failoverCalculator.multiply(a, b)).build();
        } catch (Exception e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/loadDistributorCalculator/multiply")
    @Produces(MediaType.TEXT_PLAIN)
    public Response multiplyLoadDistributor(@QueryParam("a") int a, @QueryParam("b") int b) {
        try {
            return Response.ok(loadDistributorCalculator.multiply(a, b)).build();
        } catch (Exception e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/circuitBreakerCalculator/multiply")
    @Produces(MediaType.TEXT_PLAIN)
    public Response multiplyCircuitBreaker(@QueryParam("a") int a, @QueryParam("b") int b) {
        try {
            return Response.ok(circuitBreakerCalculator.multiply(a, b)).build();
        } catch (Exception e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

}
