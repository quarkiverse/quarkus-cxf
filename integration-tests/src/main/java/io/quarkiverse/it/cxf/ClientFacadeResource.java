package io.quarkiverse.it.cxf;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.tempuri.CalculatorSoap;
import org.tempuri.alt.AltCalculatorSoap;

import io.quarkiverse.cxf.annotation.CXFClient;

@Path("/rest/clientfacade")
public class ClientFacadeResource {

    @Inject
    @CXFClient
    CalculatorSoap calculatorWS;

    @Inject
    @CXFClient
    AltCalculatorSoap altCalculatorWS;

    @GET
    @Path("/multiply")
    @Produces(MediaType.TEXT_PLAIN)
    public int multiply(
            @QueryParam("a") int a,
            @QueryParam("b") int b) {
        return calculatorWS.multiply(a, b);
    }

    @GET
    @Path("/add")
    @Produces(MediaType.TEXT_PLAIN)
    public int add(
            @QueryParam("a") int a,
            @QueryParam("b") int b) {
        return altCalculatorWS.add(a, b);
    }
}
