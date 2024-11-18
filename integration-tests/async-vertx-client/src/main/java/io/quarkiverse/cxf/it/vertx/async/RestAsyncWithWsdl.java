package io.quarkiverse.cxf.it.vertx.async;

import jakarta.enterprise.inject.Instance;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.jboss.eap.quickstarts.wscalculator.calculator.CalculatorService;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.smallrye.mutiny.Uni;

@Path("/RestAsyncWithWsdl")
public class RestAsyncWithWsdl {

    @CXFClient("calculatorWithWsdl")
    Instance<CalculatorService> calculatorWithWsdl;

    @Path("/calculatorWithWsdl")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> calculatorWithWsdl(@QueryParam("a") int a, @QueryParam("b") int b) {
        /* With WSDL and without @Blocking should fail due to blocking WSDL call on the I/O thread */
        return Uni.createFrom()
                .future(calculatorWithWsdl.get().addAsync(a, b))
                .map(addResponse -> addResponse.getReturn())
                .map(String::valueOf);
    }

}
