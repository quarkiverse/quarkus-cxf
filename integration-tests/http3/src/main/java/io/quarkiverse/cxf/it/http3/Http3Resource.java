package io.quarkiverse.cxf.it.http3;

import java.util.List;
import java.util.Map;

import jakarta.enterprise.inject.Instance;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.handler.MessageContext;

import io.quarkiverse.cxf.annotation.CXFClient;

@Path("/http3")
@Produces(MediaType.TEXT_PLAIN)
public class Http3Resource {
    @CXFClient("direct")
    Instance<VersionService> direct;
    @CXFClient("discovery")
    Instance<VersionService> discovery;
    @CXFClient("discoveryHttp1")
    Instance<VersionService> discoveryHttp1;
    @CXFClient("disabled")
    Instance<VersionService> disabled;
    @CXFClient("noAdvertisement")
    Instance<VersionService> noAdvertisement;
    @CXFClient("tcpOnly")
    Instance<VersionService> tcpOnly;
    @CXFClient("untrusted")
    Instance<VersionService> untrusted;
    @CXFClient("wrongHost")
    Instance<VersionService> wrongHost;
    @CXFClient("cleartext")
    Instance<VersionService> cleartext;
    @CXFClient("redirect")
    Instance<VersionService> redirect;
    @CXFClient("broken")
    Instance<VersionService> broken;
    @CXFClient("mutual")
    Instance<VersionService> mutual;
    @CXFClient("missingClientCertificate")
    Instance<VersionService> missingClientCertificate;

    @GET
    @Path("/{name}/{size}")
    public Response inspect(@PathParam("name") String name, @PathParam("size") int size) {
        VersionService client = switch (name) {
            case "direct" -> direct.get();
            case "discovery" -> discovery.get();
            case "discoveryHttp1" -> discoveryHttp1.get();
            case "disabled" -> disabled.get();
            case "noAdvertisement" -> noAdvertisement.get();
            case "tcpOnly" -> tcpOnly.get();
            case "untrusted" -> untrusted.get();
            case "wrongHost" -> wrongHost.get();
            case "cleartext" -> cleartext.get();
            case "redirect" -> redirect.get();
            case "broken" -> broken.get();
            case "mutual" -> mutual.get();
            case "missingClientCertificate" -> missingClientCertificate.get();
            default -> throw new IllegalArgumentException(name);
        };
        return inspect(client, size);
    }

    private Response inspect(VersionService client, int size) {
        try {
            // Exercise connection-specific headers supplied through the standard JAX-WS request context.
            ((BindingProvider) client).getRequestContext().put(MessageContext.HTTP_REQUEST_HEADERS,
                    Map.of("Connection", List.of("keep-alive"),
                            "Keep-Alive", List.of("timeout=60"), "Proxy-Connection", List.of("keep-alive"),
                            "Upgrade", List.of("h2c")));
            return Response.ok(client.inspect("x".repeat(size))).build();
        } catch (Exception e) {
            StringBuilder causes = new StringBuilder();
            for (Throwable cause = e; cause != null; cause = cause.getCause()) {
                causes.append(cause).append('\n');
            }
            return Response.serverError().entity(causes.toString()).build();
        }
    }

    @GET
    @Path("/drop")
    public Response drop() {
        VersionService client = broken.get();
        BindingProvider provider = (BindingProvider) client;
        String address = (String) provider.getRequestContext().get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY);
        provider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                address.substring(0, address.lastIndexOf('/')) + "/drop");
        return inspect(client, 1);
    }
}
