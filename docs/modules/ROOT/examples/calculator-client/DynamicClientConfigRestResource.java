package io.quarkiverse.cxf.client.it;

import java.util.Map;

import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.xml.ws.BindingProvider;

import org.jboss.eap.quickstarts.wscalculator.calculator.CalculatorService;

import io.quarkiverse.cxf.annotation.CXFClient;

/*
 * The @RequestScoped annotation causes that the REST resource is instantiated
 * anew for every call of the add() method. Therefore also a new client instance
 * is injected into the calculator field for every request served by add().
 */
@RequestScoped
@Path("/cxf/dynamic-client")
public class DynamicClientConfigRestResource {

    @CXFClient("requestScopedVertxHttpClient")
    CalculatorService calculator;

    @GET
    @Path("/add")
    @Produces(MediaType.TEXT_PLAIN)
    public int add(@QueryParam("a") int a, @QueryParam("b") int b, @QueryParam("baseUri") String baseUri) {
        Map<String, Object> ctx = ((BindingProvider) calculator).getRequestContext();
        /* We are setting the remote URL safely, because the client is associated exclusively with the current request */
        ctx.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, baseUri + "/calculator-ws/CalculatorService");
        return calculator.add(a, b);
    }

}
