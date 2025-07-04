package io.quarkiverse.cxf.perf.uuid.client;

import java.io.IOException;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkiverse.cxf.mutiny.CxfMutinyUtils;
import io.quarkiverse.cxf.perf.uuid.client.generated.EchoUuidResponse;
import io.quarkiverse.cxf.perf.uuid.client.generated.EchoUuidWs;
import io.smallrye.mutiny.Uni;

@Path("/clients")
public class EchoUuidClientResource {
    @CXFClient("echoUuidWsVertx")
    EchoUuidWs echoUuidWsVertx;

    @CXFClient("echoUuidWsUrlConnection")
    EchoUuidWs echoUuidWsUrlConnection;

    @POST
    @Path("/echoUuidWsUrlConnection/sync")
    @Produces(MediaType.TEXT_PLAIN)
    public String echoUuidWsUrlConnectionSync(String uuid) throws IOException {
        return echoUuidWsUrlConnection.echoUuid(uuid);
    }

    @POST
    @Path("/echoUuidWsVertx/sync")
    @Produces(MediaType.TEXT_PLAIN)
    public String echoUuidWsSync(String uuid) throws IOException {
        return echoUuidWsVertx.echoUuid(uuid);
    }

    @POST
    @Path("/echoUuidWsVertx/async")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> echoUuidWsAsync(String uuid) throws IOException {
        return CxfMutinyUtils
                .<EchoUuidResponse> toUni(handler -> echoUuidWsVertx.echoUuidAsync(uuid, handler))
                .map(EchoUuidResponse::getReturn);
    }

}
