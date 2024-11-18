package io.quarkiverse.cxf.it.vertx.async;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkiverse.cxf.deployment.test.HelloService;
import io.smallrye.mutiny.Uni;

@Path("/RestAsyncWithoutWsdl")
public class RestAsyncWithoutWsdl {

    @CXFClient("helloWithoutWsdl")
    HelloService helloWithoutWsdl;

    @Path("/helloWithoutWsdl")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> helloWithoutWsdl(@QueryParam("person") String person) {
        /* Without WSDL and without @Blocking should work */
        return Uni.createFrom()
                .future(helloWithoutWsdl.helloAsync(person))
                .map(helloResponse -> helloResponse.getReturn());
    }

}
