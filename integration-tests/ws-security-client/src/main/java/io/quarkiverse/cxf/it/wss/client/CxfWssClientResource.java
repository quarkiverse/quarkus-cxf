package io.quarkiverse.cxf.it.wss.client;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.jboss.eap.quickstarts.wscalculator.wsscalculator.WssCalculatorService;

import io.quarkiverse.cxf.annotation.CXFClient;

@Path("/cxf/wss-client")
public class CxfWssClientResource {

    @Inject
    @CXFClient("wss-client") // name used in application.properties
    WssCalculatorService calculator;

    @GET
    @Path("/calculator/modulo")
    @Produces(MediaType.TEXT_PLAIN)
    public int modulo(@QueryParam("a") int a, @QueryParam("b") int b) {
        return calculator.modulo(a, b);
    }

}
