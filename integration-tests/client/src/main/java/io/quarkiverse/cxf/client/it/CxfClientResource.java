package io.quarkiverse.cxf.client.it;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.xml.ws.BindingProvider;

import org.apache.cxf.common.jaxb.JAXBUtils;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.glassfish.jaxb.core.marshaller.CharacterEscapeHandler;
import org.glassfish.jaxb.runtime.v2.runtime.JAXBContextImpl;
import org.jboss.eap.quickstarts.wscalculator.calculator.CalculatorService;
import org.jboss.eap.quickstarts.wscalculator.calculator.Operands;

import io.quarkiverse.cxf.CXFClientInfo;
import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkiverse.cxf.client.it.rtinit.ClientWithRuntimeInitializedPayload;
import io.vertx.core.json.JsonObject;

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
    @CXFClient("myCalculator") // name used in application.properties
    CXFClientInfo calculatorClientInfo;

    @Inject
    @CXFClient("clientWithRuntimeInitializedPayload") // name used in application.properties
    ClientWithRuntimeInitializedPayload clientWithRuntimeInitializedPayload;

    @CXFClient("proxiedCalculator")
    CalculatorService proxiedCalculator;

    @Inject
    RequestScopedClients requestScopedClients;

    @ConfigProperty(name = "cxf.it.skewed-calculator.baseUri")
    String skewedCalculatorBaseUri;

    @GET
    @Path("/calculator/{client}/add")
    @Produces(MediaType.TEXT_PLAIN)
    public Response add(@PathParam("client") String client, @QueryParam("a") int a, @QueryParam("b") int b) {
        try {
            return Response.ok(getClient(client).add(a, b)).build();
        } catch (Exception e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

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
            case "proxiedCalculator":
                return proxiedCalculator;
            default:
                return requestScopedClients.getClient(client);
        }
    }

    @GET
    @Path("/clientInfo/{client}/{key}")
    @Produces(MediaType.TEXT_PLAIN)
    public String clientInfo(@PathParam("client") String client, @PathParam("key") String key) {
        final CalculatorService calculator = getClient(client);
        final Client cl = ClientProxy.getClient(calculator);
        final Map<String, Object> requestContext = cl.getRequestContext();
        final CXFClientInfo clientInfo = (CXFClientInfo) requestContext.get(CXFClientInfo.class.getName());

        switch (key) {
            case "wsdlUrl":
                return clientInfo.getWsdlUrl();
            case "endpointAddress":
                return clientInfo.getEndpointAddress();
            case "httpConduit":
                return cl.getConduit().getClass().getName().toString();
            case "dynamicEndpointAddress":
                Map<String, Object> ctx = ((BindingProvider) calculator).getRequestContext();
                JsonObject msg = new JsonObject();
                msg.put("urlBefore", ctx.get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY));
                ctx.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                        skewedCalculatorBaseUri + "/calculator-ws/CalculatorService");
                msg.put("result", calculator.add(3, 4));
                msg.put("urlAfter", ctx.get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY));
                return msg.encodePrettily();
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

    @GET
    @Path("/activeThreadCount")
    @Produces(MediaType.TEXT_PLAIN)
    public int activeThreadCount() {
        ThreadGroup group = Thread.currentThread().getThreadGroup();
        while (group.getParent() != null) {
            group = group.getParent();
        }
        return group.activeCount();
    }

    @POST
    @Path("/createEscapeHandler/{className}")
    @Produces(MediaType.TEXT_PLAIN)
    public String createEscapeHandler(@PathParam("className") String className, String body) throws IOException {
        CharacterEscapeHandler handler;

        switch (className) {
            case "MinimumEscapeHandler":
                handler = (CharacterEscapeHandler) JAXBUtils.createMininumEscapeHandler(JAXBContextImpl.class);
                break;
            case "NoEscapeHandler":
                handler = (CharacterEscapeHandler) JAXBUtils.createNoEscapeHandler(JAXBContextImpl.class);
                break;
            default:
                throw new RuntimeException("Unexpected className " + className);
        }
        StringWriter out = new StringWriter();
        char[] bodyChars = body.toCharArray();
        handler.escape(bodyChars, 0, bodyChars.length, false, out);
        return out.toString();
    }

    @RequestScoped
    public static class RequestScopedClients {

        @CXFClient("requestScopedVertxHttpClient")
        CalculatorService requestScopedVertxHttpClient;

        @CXFClient("requestScopedUrlConnectionClient")
        CalculatorService requestScopedUrlConnectionClient;

        public CalculatorService getClient(String client) {
            switch (client) {
                case "requestScopedVertxHttpClient":
                    return requestScopedVertxHttpClient;
                case "requestScopedUrlConnectionClient":
                    return requestScopedUrlConnectionClient;
                default:
                    throw new IllegalStateException("Unexpected client key " + client);
            }
        }

    }
}
