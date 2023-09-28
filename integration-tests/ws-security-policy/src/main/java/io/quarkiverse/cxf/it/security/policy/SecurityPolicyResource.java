package io.quarkiverse.cxf.it.security.policy;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkiverse.cxf.it.security.policy.BeanProducers.MessageCollector;

@Path("/cxf/security-policy")
public class SecurityPolicyResource {

    @Inject
    @CXFClient("hello")
    HelloService hello;

    @Inject
    @CXFClient("helloIp")
    HelloService helloIp;

    @Inject
    @CXFClient("helloHttps")
    HelloService helloHttps;

    @Inject
    @CXFClient("helloHttp")
    HelloService helloHttp;

    @Inject
    @CXFClient("helloUsernameToken")
    HelloService helloUsernameToken;

    @Inject
    @CXFClient("helloNoUsernameToken")
    HelloService helloNoUsernameToken;

    @Inject
    @Named("messageCollector")
    MessageCollector messageCollector;

    @GET
    @Path("/drainMessages")
    @Produces(MediaType.TEXT_PLAIN)
    public String drainMessages() {
        return messageCollector.drainMessages().stream().collect(Collectors.joining("|||"));
    }

    @POST
    @Path("/{client}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response hello(@PathParam("client") String client, String body) {
        final HelloService service;
        switch (client) {
            case "hello":
                service = hello;
                break;
            case "helloIp":
                service = helloIp;
                break;
            case "helloHttps":
                service = helloHttps;
                break;
            case "helloHttp":
                service = helloHttp;
                break;
            case "helloUsernameToken":
                service = helloUsernameToken;
                break;
            case "helloNoUsernameToken":
                service = helloNoUsernameToken;
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
