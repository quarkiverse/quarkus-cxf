package io.quarkiverse.cxf.it.security.policy;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
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
    HelloIpService helloIpService;

    @POST
    @Path("/hello")
    @Produces(MediaType.TEXT_PLAIN)
    public String hello(String body) {
        return helloService.hello(body);
    }

    @POST
    @Path("/helloIp")
    @Produces(MediaType.TEXT_PLAIN)
    public Response helloIp(String body) {
        try {
            return Response.ok(helloIpService.hello(body)).build();
        } catch (Exception e) {
            Throwable cause = e;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            return Response.status(500).entity(cause.getMessage()).build();
        }
    }

}
