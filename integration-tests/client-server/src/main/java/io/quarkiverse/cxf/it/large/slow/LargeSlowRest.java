package io.quarkiverse.cxf.it.large.slow;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkiverse.cxf.it.large.slow.generated.LargeSlowResponse;
import io.quarkiverse.cxf.it.large.slow.generated.LargeSlowService;
import io.quarkiverse.cxf.mutiny.CxfMutinyUtils;
import io.smallrye.mutiny.Uni;

@Path("/LargeSlowRest")
public class LargeSlowRest {

    @CXFClient("largeSlow")
    LargeSlowService largeSlow;

    @Path("/largeHelloAsync")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> largeHelloAsync(@QueryParam("sizeBytes") int sizeBytes, @QueryParam("delayMs") int delayMs) {
        return CxfMutinyUtils
                .<LargeSlowResponse> toUni(handler -> largeSlow.largeSlowAsync(sizeBytes, delayMs, handler))
                .map(response -> response.getReturn().getPayload());
    }

    @Path("/largeHelloSync")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String largeHelloSync(@QueryParam("sizeBytes") int sizeBytes, @QueryParam("delayMs") int delayMs) {
        return largeSlow.largeSlow(sizeBytes, delayMs).getPayload();
    }

}
