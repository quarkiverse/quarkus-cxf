package io.quarkiverse.cxf.it.ws.rm.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkiverse.cxf.it.ws.rm.server.WsrmHelloService;

@Path("/wsrm-rest")
public class WsrmResource {

    @Inject
    @CXFClient("wsrm")
    WsrmHelloService wsrmHelloService;

    @Inject
    @Named
    InMessageRecorder inMessageRecorder;

    @Inject
    @Named
    OutMessageRecorder outMessageRecorder;

    @POST
    @Path("/hello")
    @Produces(MediaType.TEXT_PLAIN)
    public String hello(String body) throws IOException {
        return wsrmHelloService.hello(body);
    }

    @GET
    @Path("/messages/{direction}")
    @Produces(MediaType.TEXT_PLAIN)
    public String messages(@PathParam("direction") String direction) throws IOException {
        List<byte[]> messages = "out".equals(direction) ? outMessageRecorder.drainMessages()
                : inMessageRecorder.drainMessages();
        if (messages.isEmpty()) {
            return null;
        } else {
            StringBuilder sb = new StringBuilder();
            for (byte[] bs : messages) {
                if (sb.length() > 0) {
                    sb.append("|||");
                }
                sb.append(new String(bs, StandardCharsets.UTF_8));
            }
            return sb.toString();
        }
    }

}
