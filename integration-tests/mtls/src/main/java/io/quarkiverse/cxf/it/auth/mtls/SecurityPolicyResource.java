package io.quarkiverse.cxf.it.auth.mtls;

import java.io.PrintWriter;
import java.io.StringWriter;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.quarkiverse.cxf.annotation.CXFClient;

@Path("/cxf/mtls-rest")
public class SecurityPolicyResource {

    @CXFClient("mTls")
    HelloService mTls;

    @CXFClient("mTlsOld")
    HelloService mTlsOld;

    @CXFClient("noKeystore")
    HelloService noKeystore;

    @POST
    @Path("/{client}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response hello(@PathParam("client") String client, String body) {
        final HelloService service;
        switch (client) {
            case "mTls":
                service = mTls;
                break;
            case "mTlsOld":
                service = mTlsOld;
                break;
            case "noKeystore":
                service = noKeystore;
                break;
            default:
                throw new IllegalStateException("Unexpected client " + client);
        }
        try {
            return Response.ok(service.hello(body)).build();
        } catch (Exception e) {
            final StringWriter w = new StringWriter();
            final PrintWriter pw = new PrintWriter(w);
            e.printStackTrace(pw);
            return Response.status(500).entity(w.toString()).build();
        }
    }
}
