package io.quarkiverse.cxf.client.it;

import java.io.InputStream;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.eap.quickstarts.wscalculator.calculator.CalculatorService;
import org.jboss.eap.quickstarts.wscalculator.calculator.Operands;

import io.quarkiverse.cxf.CXFClientInfo;
import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkiverse.cxf.client.it.rtinit.ClientWithRuntimeInitializedPayload;

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

    @Inject
    @CXFClient("clientWithRuntimeInitializedPayload") // name used in application.properties
    ClientWithRuntimeInitializedPayload clientWithRuntimeInitializedPayload;

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
    public int codeFirstClient(@QueryParam("a") int a, @QueryParam("b") int b) {
        return codeFirstClient.multiply(a, b);
    }

    @GET
    @Path("/clientWithRuntimeInitializedPayload/addOperands")
    @Produces(MediaType.TEXT_PLAIN)
    public int clientWithRuntimeInitializedPayload(@QueryParam("a") int a, @QueryParam("b") int b) {
        return clientWithRuntimeInitializedPayload.addOperands(new io.quarkiverse.cxf.client.it.rtinit.Operands(a, b))
                .getResult();
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

    @GET
    @Path("/resource/{path : .+}")
    @Produces(MediaType.TEXT_PLAIN)
    public InputStream resource(@PathParam("path") String path) {
        return getClass().getClassLoader().getResourceAsStream(path);
    }
}
