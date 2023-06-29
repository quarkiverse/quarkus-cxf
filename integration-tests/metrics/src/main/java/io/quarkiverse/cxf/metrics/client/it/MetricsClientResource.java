package io.quarkiverse.cxf.metrics.client.it;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkiverse.cxf.metrics.server.it.HelloService;

@Path("/metrics/client")
public class MetricsClientResource {

    @Inject
    @CXFClient("hello")
    HelloService helloClient;

    @POST
    @Path("/hello")
    @Produces(MediaType.TEXT_PLAIN)
    public String createEscapeHandler(String body) throws IOException {
        return helloClient.hello(body);
    }

}
