package io.quarkiverse.cxf.client.it;

import java.io.InputStream;
import java.util.List;

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

    @GET
    @Path("/calculator/{client}/addNumberAndOperands")
    @Produces(MediaType.TEXT_PLAIN)
    public int addNumberAndOperands(@PathParam("client") String client, @QueryParam("a") int a, @QueryParam("b") int b,
            @QueryParam("c") int c) {
        final Operands ops = new Operands();
        ops.setA(b);
        ops.setB(c);
        return getClient(client).addNumberAndOperands(a, ops);
    }

    @GET
    @Path("/calculator/{client}/addArray")
    @Produces(MediaType.TEXT_PLAIN)
    public int addArray(@PathParam("client") String client, @QueryParam("a") int a, @QueryParam("b") int b,
            @QueryParam("c") int c) {
        return getClient(client).addArray(List.of(a, b, c));
    }

    @GET
    @Path("/calculator/{client}/addList")
    @Produces(MediaType.TEXT_PLAIN)
    public int addList(@PathParam("client") String client, @QueryParam("a") int a, @QueryParam("b") int b,
            @QueryParam("c") int c) {
        return getClient(client).addList(List.of(a, b, c));
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
