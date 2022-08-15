package io.quarkiverse.cxf.client.it;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.eap.quickstarts.wscalculator.calculator.CalculatorService;
import org.jboss.eap.quickstarts.wscalculator.calculator.Operands;

import io.quarkiverse.cxf.CXFClientInfo;
import io.quarkiverse.cxf.annotation.CXFClient;

@Path("/cxf/client")
public class CxfClientResource {

    @Inject
    @CXFClient // no explicit client name here
    CalculatorService defaultCalculatorService;

    @Inject
    @CXFClient("myCalculator") // name used in application.properties
    CalculatorService myCalculator;

    @Inject
    @CXFClient("mySkewedCalculator") // name used in application.properties
    CalculatorService mySkewedCalculator;

    @Inject
    @CXFClient("codeFirstClient") // name used in application.properties
    CodeFirstClient codeFirstClient;

    @Inject
    @CXFClient("myFaultyCalculator") // name used in application.properties
    CalculatorService myFaultyCalculator;

    @Inject
    @Named("org.jboss.eap.quickstarts.wscalculator.calculator.CalculatorService")
    CXFClientInfo calculatorClientInfo;

    @GET
    @Path("/calculator/{client}/multiply")
    @Produces(MediaType.TEXT_PLAIN)
    public Response multiplyDefault(@PathParam("client") String client, @QueryParam("a") int a, @QueryParam("b") int b) {
        try {
            return Response.ok(getClient(client).multiply(a, b)).build();
        } catch (Exception e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/codeFirstClient/multiply")
    @Produces(MediaType.TEXT_PLAIN)
    public int codeFirstClient(@PathParam("client") String client, @QueryParam("a") int a, @QueryParam("b") int b) {
        return codeFirstClient.multiply(a, b);
    }

    @GET
    @Path("/calculator/{client}/addOperands")
    @Produces(MediaType.TEXT_PLAIN)
    public int addOperands(@PathParam("client") String client, @QueryParam("a") int a, @QueryParam("b") int b) {
        final Operands ops = new Operands();
        ops.setA(a);
        ops.setB(b);
        return getClient(client).addOperands(ops).getResult();
    }

    private CalculatorService getClient(String client) {
        switch (client) {
            case "default":
                return defaultCalculatorService;
            case "myCalculator":
                return myCalculator;
            case "myFaultyCalculator":
                return myFaultyCalculator;
            case "mySkewedCalculator":
                return mySkewedCalculator;
            default:
                throw new IllegalStateException("Unexpected client key " + client);
        }
    }

    @GET
    @Path("/clientInfo/{client}/{key}")
    @Produces(MediaType.TEXT_PLAIN)
    public String clientInfo(@PathParam("client") String client, @PathParam("key") String key) {

        CXFClientInfo clientInfo = null;
        switch (client) {
            case "myCalculator":
                clientInfo = calculatorClientInfo;
                break;
            default:
                throw new IllegalStateException("Unexpected client key " + client);
        }

        switch (key) {
            case "wsdlUrl":
                return clientInfo.getWsdlUrl();
            case "endpointAddress":
                return clientInfo.getEndpointAddress();
            default:
                throw new IllegalStateException("Unexpected client key " + client);
        }

    }

}
