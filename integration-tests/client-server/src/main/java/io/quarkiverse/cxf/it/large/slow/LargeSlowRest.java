package io.quarkiverse.cxf.it.large.slow;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkiverse.cxf.it.large.slow.generated.LargeSlowResponse;
import io.quarkiverse.cxf.it.large.slow.generated.LargeSlowService;
import io.quarkiverse.cxf.mutiny.CxfMutinyUtils;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;

@Path("/LargeSlowRest")
public class LargeSlowRest {

    @CXFClient("largeSlow")
    LargeSlowService largeSlow;

    @CXFClient("largeSlowReceiveTimeout")
    LargeSlowService largeSlowReceiveTimeout;

    @Path("/async/{client}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Response> largeHelloAsync(
            @PathParam("client") String client,
            @QueryParam("sizeBytes") int sizeBytes,
            @QueryParam("clientDeserializationDelayMs") int clientDeserializationDelayMs,
            @QueryParam("serviceExecutionDelayMs") int serviceExecutionDelayMs) {
        Log.infof("Invoking async %s client with clientDeserializationDelayMs %s and serviceExecutionDelayMs %d", client,
                clientDeserializationDelayMs, serviceExecutionDelayMs);
        return CxfMutinyUtils
                .<LargeSlowResponse> toUni(handler -> getClient(client)
                        .largeSlowAsync(sizeBytes, clientDeserializationDelayMs, serviceExecutionDelayMs, handler))
                .map(response -> Response.ok(response.getReturn().getPayload()).build())
                .onFailure().recoverWithItem(e -> {
                    return Response.serverError().entity(rootCause(e).getMessage()).build();
                });
    }

    @Path("/sync/{client}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response largeHelloSync(
            @PathParam("client") String client,
            @QueryParam("sizeBytes") int sizeBytes,
            @QueryParam("clientDeserializationDelayMs") int clientDeserializationDelayMs,
            @QueryParam("serviceExecutionDelayMs") int serviceExecutionDelayMs) {
        Log.infof("Invoking sync %s client with clientDeserializationDelayMs %s and serviceExecutionDelayMs %d", client,
                clientDeserializationDelayMs, serviceExecutionDelayMs);
        try {
            return Response.ok(
                    getClient(client).largeSlow(sizeBytes, clientDeserializationDelayMs, serviceExecutionDelayMs).getPayload())
                    .build();
        } catch (Exception e) {
            return Response.serverError().entity(rootCause(e).getMessage()).build();
        }
    }

    LargeSlowService getClient(String client) {
        switch (client) {
            case "largeSlow": {
                return largeSlow;
            }
            case "largeSlowReceiveTimeout": {
                return largeSlowReceiveTimeout;
            }
            default:
                throw new IllegalArgumentException("Unexpected client: " + client);
        }
    }

    private static Throwable rootCause(Throwable e) {
        e.printStackTrace();
        Throwable result = e;
        while (result.getCause() != null) {
            result = result.getCause();
        }
        return result;
    }
}
