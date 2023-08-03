// tag::quarkus-cxf-rt-transports-http-hc5.usage.mutiny[]
package io.quarkiverse.cxf.hc5.it;

import java.util.concurrent.Future;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.jboss.eap.quickstarts.wscalculator.calculator.AddResponse;
import org.jboss.eap.quickstarts.wscalculator.calculator.CalculatorService;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.smallrye.mutiny.Uni;

@Path("/hc5")
public class Hc5Resource {

    @Inject
    @CXFClient("myCalculator") // name used in application.properties
    CalculatorService myCalculator;

    @SuppressWarnings("unchecked")
    @Path("/add-async")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Integer> addAsync(@QueryParam("a") int a, @QueryParam("b") int b) {
        return Uni.createFrom()
                .future(
                        (Future<AddResponse>) myCalculator
                                .addAsync(a, b, res -> {
                                }))
                .map(addResponse -> addResponse.getReturn());
    }

    // end::quarkus-cxf-rt-transports-http-hc5.usage.mutiny[]
    @Path("/add-sync")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public int addSync(@QueryParam("a") int a, @QueryParam("b") int b) {
        return myCalculator.add(a, b);
    }
    // tag::quarkus-cxf-rt-transports-http-hc5.usage.mutiny[]
}
// end::quarkus-cxf-rt-transports-http-hc5.usage.mutiny[]
