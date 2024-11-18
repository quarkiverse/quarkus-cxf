package io.quarkiverse.cxf.it.vertx.async;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.jboss.eap.quickstarts.wscalculator.calculator.CalculatorService;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;

@Path("/RestAsyncWithWsdlWithBlocking")
public class RestAsyncWithWsdlWithBlocking {

    @CXFClient("calculatorWithWsdlWithBlocking")
    CalculatorService calculatorWithWsdlWithBlocking;

    @Path("/calculatorWithWsdlWithBlocking")
    @GET
    @Blocking
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> calculatorWithWsdlWithBlocking(@QueryParam("a") int a, @QueryParam("b") int b) {
        /* With WSDL and with @Blocking should work */
        return Uni.createFrom()
                .future(calculatorWithWsdlWithBlocking.addAsync(a, b))
                .map(addResponse -> addResponse.getReturn())
                .map(String::valueOf);
    }

}
