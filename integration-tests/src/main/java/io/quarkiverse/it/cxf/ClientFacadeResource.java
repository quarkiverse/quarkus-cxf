package io.quarkiverse.it.cxf;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.tempuri.Calculator;

@Path("/rest/clientfacade")
public class ClientFacadeResource {

    @Inject
    Calculator calculatorWS;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public int multiply(@QueryParam("a") int a, @QueryParam("b") int b) {
        return calculatorWS.multiply(a, b);
    }
}
