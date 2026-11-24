package io.quarkiverse.cxf.it.http3;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.xml.ws.BindingProvider;

import io.quarkiverse.cxf.annotation.CXFClient;

@Path("/http3")
public class Http3Resource {

    @CXFClient("h3")
    ProtocolEcho h3;

    @CXFClient("h3Fallback")
    ProtocolEcho h3Fallback;

    @CXFClient("h3FallbackH2")
    ProtocolEcho h3FallbackH2;

    @CXFClient("h3FallbackLocalhost")
    ProtocolEcho h3FallbackLocalhost;

    @CXFClient("h3MutualTls")
    ProtocolEcho h3MutualTls;

    @CXFClient("h3TrustAll")
    ProtocolEcho h3TrustAll;

    @CXFClient("h3Untrusted")
    ProtocolEcho h3Untrusted;

    @CXFClient("h3Redirect")
    ProtocolEcho h3Redirect;

    @CXFClient("h3EndpointOverride")
    ProtocolEcho h3EndpointOverride;

    @CXFClient("h3EndpointOverrideNoHttp11")
    ProtocolEcho h3EndpointOverrideNoHttp11;

    @CXFClient("h3ReceiveTimeout")
    ProtocolEcho h3ReceiveTimeout;

    @CXFClient("h3ConnectionTimeout")
    ProtocolEcho h3ConnectionTimeout;

    @CXFClient("h3KeepAliveTimeout")
    ProtocolEcho h3KeepAliveTimeout;

    @CXFClient("h3OtherKeepAliveTimeouts")
    ProtocolEcho h3OtherKeepAliveTimeouts;

    @CXFClient("h3MaxSize1")
    ProtocolEcho h3MaxSize1;

    @CXFClient("h3MaxSize3")
    ProtocolEcho h3MaxSize3;

    @CXFClient("h3Metrics")
    ProtocolEcho h3Metrics;

    @CXFClient("h3Service")
    ProtocolEcho h3Service;

    @CXFClient("h3ServiceMutualTls")
    ProtocolEcho h3ServiceMutualTls;

    @CXFClient("h3ServiceFallback")
    ProtocolEcho h3ServiceFallback;

    @POST
    @Path("/echo/{client}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response echo(@PathParam("client") String client, @QueryParam("endpoint") String endpoint, String body) {
        final ProtocolEcho echo = client(client);
        if (endpoint != null) {
            ((BindingProvider) echo).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpoint);
        }
        try {
            return Response.ok(echo.echo(body)).build();
        } catch (Exception e) {
            return failure(e);
        }
    }

    @POST
    @Path("/delayedEcho/{client}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response delayedEcho(@PathParam("client") String client, String delayMs) {
        try {
            return Response.ok(client(client).delayedEcho(Long.parseLong(delayMs))).build();
        } catch (Exception e) {
            return failure(e);
        }
    }

    /**
     * Calls {@link ProtocolEcho#generate(int)} and summarizes its possibly large response.
     *
     * @param client the name of the client to use
     * @param length the number of characters the service should generate
     * @return the HTTP protocol returned by the service, followed by a colon and the number of {@code x} characters
     *         returned by the service; or HTTP status 500 if the call failed or if the service returned anything else
     *         than {@code x} characters after the protocol
     * @since 4.0.0
     */
    @POST
    @Path("/generate/{client}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response generate(@PathParam("client") String client, String length) {
        try {
            final String result = client(client).generate(Integer.parseInt(length));
            final int colon = result.indexOf(':');
            final String payload = result.substring(colon + 1);
            if (!payload.equals("x".repeat(payload.length()))) {
                return Response.status(500).entity("Unexpected characters in the generated payload").build();
            }
            return Response.ok(result.substring(0, colon) + ":" + payload.length()).build();
        } catch (Exception e) {
            return failure(e);
        }
    }

    /**
     * Calls {@link ProtocolEcho#connectionId(long)} {@code count} times concurrently.
     *
     * @param client the name of the client to use
     * @param count the number of concurrent calls
     * @param holdMs the number of milliseconds for which the service should hold each request
     * @return a comma separated list of the values returned by the individual calls
     */
    @POST
    @Path("/connectionIds/{client}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response connectionIds(@PathParam("client") String client, @QueryParam("count") int count, String holdMs) {
        final ProtocolEcho echo = client(client);
        final long hold = Long.parseLong(holdMs);
        final ExecutorService executor = Executors.newFixedThreadPool(count);
        try {
            final List<Future<String>> futures = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                futures.add(executor.submit(() -> echo.connectionId(hold)));
            }
            final List<String> results = new ArrayList<>(count);
            for (Future<String> future : futures) {
                results.add(future.get());
            }
            return Response.ok(String.join(",", results)).build();
        } catch (Exception e) {
            return failure(e);
        } finally {
            executor.shutdownNow();
        }
    }

    private ProtocolEcho client(String client) {
        return switch (client) {
            case "h3" -> h3;
            case "h3Fallback" -> h3Fallback;
            case "h3FallbackH2" -> h3FallbackH2;
            case "h3FallbackLocalhost" -> h3FallbackLocalhost;
            case "h3MutualTls" -> h3MutualTls;
            case "h3TrustAll" -> h3TrustAll;
            case "h3Untrusted" -> h3Untrusted;
            case "h3Redirect" -> h3Redirect;
            case "h3EndpointOverride" -> h3EndpointOverride;
            case "h3EndpointOverrideNoHttp11" -> h3EndpointOverrideNoHttp11;
            case "h3ReceiveTimeout" -> h3ReceiveTimeout;
            case "h3ConnectionTimeout" -> h3ConnectionTimeout;
            case "h3KeepAliveTimeout" -> h3KeepAliveTimeout;
            case "h3OtherKeepAliveTimeouts" -> h3OtherKeepAliveTimeouts;
            case "h3MaxSize1" -> h3MaxSize1;
            case "h3MaxSize3" -> h3MaxSize3;
            case "h3Metrics" -> h3Metrics;
            case "h3Service" -> h3Service;
            case "h3ServiceMutualTls" -> h3ServiceMutualTls;
            case "h3ServiceFallback" -> h3ServiceFallback;
            default -> throw new IllegalArgumentException("Unexpected client " + client);
        };
    }

    private static Response failure(Exception e) {
        final StringWriter w = new StringWriter();
        e.printStackTrace(new PrintWriter(w));
        return Response.status(500).entity(w.toString()).build();
    }
}
