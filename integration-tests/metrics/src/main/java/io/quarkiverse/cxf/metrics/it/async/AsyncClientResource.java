// tag::quarkus-cxf-rt-transports-http-hc5.usage.mutiny[]
package io.quarkiverse.cxf.metrics.it.async;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.jboss.eap.quickstarts.wscalculator.calculator.AddResponse;
import org.jboss.eap.quickstarts.wscalculator.calculator.CalculatorService;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkiverse.cxf.mutiny.CxfMutinyUtils;
import io.smallrye.mutiny.Uni;

@Path("/hc5")
public class AsyncClientResource {

    @Inject
    @CXFClient("myCalculator") // name used in application.properties
    CalculatorService myCalculator;

    @SuppressWarnings("unchecked")
    @Path("/add-async")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Integer> addAsync(@QueryParam("a") int a, @QueryParam("b") int b) {
        return CxfMutinyUtils
                .<AddResponse> toUni(handler -> myCalculator.addAsync(a, b, handler))
                .map(AddResponse::getReturn);
    }

    // end::quarkus-cxf-rt-transports-http-hc5.usage.mutiny[]
    @Path("/add-sync")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public int addSync(@QueryParam("a") int a, @QueryParam("b") int b) {
        return myCalculator.add(a, b);
    }

    @Inject
    @CXFClient("observableCalculator")
    CalculatorService observableCalculator;

    @SuppressWarnings("unchecked")
    @Path("/add-async-observable")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Integer> addAsyncObservable(@QueryParam("a") int a, @QueryParam("b") int b) {
        return CxfMutinyUtils
                .<AddResponse> toUni(handler -> observableCalculator.addAsync(a, b, handler))
                .map(AddResponse::getReturn);
    }

    @Path("/add-sync-observable")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public int addSyncObservable(@QueryParam("a") int a, @QueryParam("b") int b) {
        return observableCalculator.add(a, b);
    }

    @Inject
    @CXFClient("contextPropagationCalculator")
    CalculatorService contextPropagationCalculator;

    @SuppressWarnings("unchecked")
    @Path("/add-async-contextPropagation")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Integer> addAsyncContextPropagation(@QueryParam("a") int a, @QueryParam("b") int b) {
        return CxfMutinyUtils
                .<AddResponse> toUni(handler -> contextPropagationCalculator.addAsync(a, b, handler))
                .map(AddResponse::getReturn);
    }

    @Path("/add-sync-contextPropagation")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public int addSyncContextPropagation(@QueryParam("a") int a, @QueryParam("b") int b) {
        return contextPropagationCalculator.add(a, b);
    }

    @Path("/conduit")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String conduit() {
        final Client client = ClientProxy.getClient(myCalculator);
        return client.getConduit().getClass().getName();
    }

    // tag::quarkus-cxf-rt-transports-http-hc5.usage.mutiny[]
}
// end::quarkus-cxf-rt-transports-http-hc5.usage.mutiny[]
