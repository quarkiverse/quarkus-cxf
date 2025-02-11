package io.quarkiverse.cxf.it.vertx.async;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkiverse.cxf.deployment.test.HelloResponse;
import io.quarkiverse.cxf.deployment.test.HelloService;
import io.quarkiverse.cxf.mutiny.CxfMutinyUtils;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;

@Path("/RestAsyncWithWsdlWithBlocking")
public class RestAsyncWithWsdlWithBlocking {

    @CXFClient("helloWithWsdlWithBlocking")
    HelloService helloWithWsdlWithBlocking;

    @Path("/helloWithWsdlWithBlocking")
    @POST
    @Blocking
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> helloWithWsdlWithBlocking(String body) {
        /* With WSDL and with @Blocking should work */
        return CxfMutinyUtils
                .<HelloResponse> toUni(handler -> helloWithWsdlWithBlocking.helloAsync(body, handler))
                .map(HelloResponse::getReturn);
    }

}
