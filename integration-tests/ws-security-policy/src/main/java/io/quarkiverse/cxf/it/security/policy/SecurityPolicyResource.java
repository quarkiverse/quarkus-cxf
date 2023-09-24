package io.quarkiverse.cxf.it.security.policy;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.quarkiverse.cxf.annotation.CXFClient;

@Path("/cxf/security-policy")
public class SecurityPolicyResource {

    @Inject
    @CXFClient("hello")
    HelloService helloService;

    @Inject
    @CXFClient("helloIp")
    HelloService helloIpService;

    @Inject
    @CXFClient("helloPolicyHttps")
    HelloService helloPolicyHttpsService;

    @Inject
    @CXFClient("helloPolicyHttp")
    HelloService helloPolicyHttpService;

    @POST
    @Path("/{client}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response hello(@PathParam("client") String client, String body) {
        final HelloService service;
        switch (client) {
            case "hello":
                service = helloService;
                break;
            case "helloIp":
                service = helloIpService;
                break;
            case "helloPolicyHttps":
                service = helloPolicyHttpsService;
                break;
            case "helloPolicyHttp":
                service = helloPolicyHttpService;
                break;
            default:
                throw new IllegalStateException("Unexpected client " + client);
        }
        try {
            return Response.ok(service.hello(body)).build();
        } catch (Exception e) {
            Throwable cause = e;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            return Response.status(500).entity(cause.getMessage()).build();
        }
    }
}
