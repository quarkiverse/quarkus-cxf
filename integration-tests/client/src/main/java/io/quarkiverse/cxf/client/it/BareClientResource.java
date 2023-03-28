package io.quarkiverse.cxf.client.it;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.eap.quickstarts.wscalculator.barecalculator.BareCalculatorService;
import org.jboss.eap.quickstarts.wscalculator.barecalculator.Operands;

import io.quarkiverse.cxf.annotation.CXFClient;
import net.java.dev.jaxb.array.LongArray;

@Path("/cxf/client/bare")
public class BareClientResource {
    @Inject
    @CXFClient("myBareCalculatorService") // name used in application.properties
    BareCalculatorService myBareCalculatorService;

    @GET
    @Path("/addOperands")
    @Produces(MediaType.TEXT_PLAIN)
    public Response addOperands(@QueryParam("a") int a, @QueryParam("b") int b)
            throws IOException {
        final Operands ops = new Operands();
        ops.setA(a);
        ops.setB(b);
        try {
            return Response.ok(myBareCalculatorService.addOperands(ops).getResult()).build();
        } catch (Exception e) {
            try (StringWriter stackTrace = new StringWriter(); PrintWriter out = new PrintWriter(stackTrace)) {
                e.printStackTrace(out);
                return Response.serverError().entity(stackTrace.toString()).build();
            }
        }
    }

    @GET
    @Path("/echo")
    @Produces(MediaType.TEXT_PLAIN)
    public Response echo(@QueryParam("a") int a)
            throws IOException {
        try {
            return Response.ok(myBareCalculatorService.echo(a)).build();
        } catch (Exception e) {
            try (StringWriter stackTrace = new StringWriter(); PrintWriter out = new PrintWriter(stackTrace)) {
                e.printStackTrace(out);
                return Response.serverError().entity(stackTrace.toString()).build();
            }
        }
    }

    @GET
    @Path("/addArray")
    @Produces(MediaType.TEXT_PLAIN)
    public Response addArray(@QueryParam("a") long a, @QueryParam("b") long b, @QueryParam("c") long c)
            throws IOException {

        try {
            LongArray array = new LongArray();
            array.getItem().add(a);
            array.getItem().add(b);
            array.getItem().add(c);
            return Response.ok(myBareCalculatorService.addArray(array)).build();
        } catch (Exception e) {
            try (StringWriter stackTrace = new StringWriter(); PrintWriter out = new PrintWriter(stackTrace)) {
                e.printStackTrace(out);
                return Response.serverError().entity(stackTrace.toString()).build();
            }
        }
    }

}
