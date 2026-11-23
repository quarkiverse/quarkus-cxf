package io.quarkiverse.cxf.metrics.client.it;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.jboss.eap.quickstarts.wscalculator.calculator.AddResponse;
import org.jboss.eap.quickstarts.wscalculator.calculator.CalculatorService;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkiverse.cxf.mutiny.CxfMutinyUtils;
import io.smallrye.mutiny.Uni;

/**
 * Kept separate from {@link MetricsClientResource}, because this resource has a reactive method that runs on the
 * Vert.x event loop, where the resource bean and its injected clients may get created.
 * Clients with a remote WSDL (such as {@code hello}) must not be injected here,
 * because they fetch the WSDL using a blocking call, which is not allowed on the event loop.
 */
@Path("/metrics/client")
public class VertxCalculatorClientResource {

    @Inject
    @CXFClient("vertxCalculator")
    CalculatorService vertxCalculator;

    @SuppressWarnings("unchecked")
    @Path("/addAsync")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Integer> addAsync(@QueryParam("a") int a, @QueryParam("b") int b) {
        return CxfMutinyUtils
                .<AddResponse> toUni(handler -> vertxCalculator.addAsync(a, b, handler))
                .map(AddResponse::getReturn);
    }

}
