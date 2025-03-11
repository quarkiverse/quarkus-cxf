package io.quarkiverse.cxf.it.redirect;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.soap.SOAPFaultException;

import org.apache.cxf.message.Message;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkiverse.cxf.it.large.slow.generated.LargeSlowResponse;
import io.quarkiverse.cxf.it.large.slow.generated.LargeSlowService;
import io.quarkiverse.cxf.it.redirect.retransmitcache.RetransmitCacheOutput;
import io.quarkiverse.cxf.it.redirect.retransmitcache.RetransmitCacheResponse;
import io.quarkiverse.cxf.it.redirect.retransmitcache.RetransmitCacheService;
import io.quarkiverse.cxf.it.redirect.retransmitcache.RetransmitCacheServiceImpl;
import io.quarkiverse.cxf.mutiny.CxfMutinyUtils;
import io.quarkiverse.cxf.mutiny.FailedResponse;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;

@Path("/RedirectRest")
public class RedirectRest {

    private final AtomicInteger redirectCounter = new AtomicInteger(0);

    @CXFClient("singleRedirect")
    LargeSlowService singleRedirect;

    @CXFClient("doubleRedirect")
    LargeSlowService doubleRedirect;

    @CXFClient("tripleRedirect")
    LargeSlowService tripleRedirect;

    @CXFClient("noAutoRedirect")
    LargeSlowService noAutoRedirect;

    @CXFClient("doubleRedirectMaxRetransmits1")
    LargeSlowService doubleRedirectMaxRetransmits1;

    @CXFClient("doubleRedirectMaxRetransmits2")
    LargeSlowService doubleRedirectMaxRetransmits2;

    @CXFClient("doubleRedirectMaxRetransmits2MaxSameUri0")
    LargeSlowService doubleRedirectMaxRetransmits2MaxSameUri0;

    @CXFClient("maxSameUri1")
    LargeSlowService maxSameUri1;

    @CXFClient("maxSameUri2")
    LargeSlowService maxSameUri2;

    @CXFClient("maxSameUri3")
    LargeSlowService maxSameUri3;

    @CXFClient("loop")
    LargeSlowService loop;

    @CXFClient("retransmitCache")
    RetransmitCacheService retransmitCache;

    @ConfigProperty(name = "qcxf.retransmitCacheDir")
    String retransmitCacheDir;

    LargeSlowService getClient(String clientName) {
        switch (clientName) {
            case "singleRedirect": {
                return singleRedirect;
            }
            case "doubleRedirect": {
                return doubleRedirect;
            }
            case "tripleRedirect": {
                return tripleRedirect;
            }
            case "noAutoRedirect": {
                return noAutoRedirect;
            }
            case "doubleRedirectMaxRetransmits1": {
                return doubleRedirectMaxRetransmits1;
            }
            case "doubleRedirectMaxRetransmits2": {
                return doubleRedirectMaxRetransmits2;
            }
            case "doubleRedirectMaxRetransmits2MaxSameUri0": {
                return doubleRedirectMaxRetransmits2MaxSameUri0;
            }
            case "maxSameUri1": {
                return maxSameUri1;
            }
            case "maxSameUri2": {
                return maxSameUri2;
            }
            case "maxSameUri3": {
                return maxSameUri3;
            }
            case "loop": {
                return loop;
            }
            default:
                throw new IllegalArgumentException("Unexpected client name: " + clientName);
        }
    }

    @Path("/singleRedirect")
    @POST
    public Response singleRedirect(String body) {
        // Log.infof("/RedirectRest/singleRedirect Received payload %s", body);
        // Relative URI: return Response.status(302).header("Location", "/soap/largeSlow").build();
        // RestEASY will convert this to an absolute URI
        return Response.temporaryRedirect(URI.create("/soap/largeSlow")).build();
    }

    @Path("/doubleRedirect")
    @POST
    public Response doubleRedirect(String body) {
        // Log.infof("/RedirectRest/doubleRedirect Received payload %s", body);
        return Response.status(302).header("Location", "/RedirectRest/singleRedirect").build();
    }

    @Path("/tripleRedirect")
    @POST
    public Response tripleRedirect() {
        return Response.status(302).header("Location", "/RedirectRest/doubleRedirect").build();
    }

    @Path("/selfRedirect")
    @DELETE
    public void selfRedirect() {
        Log.info("Resetting redirectCounter");
        redirectCounter.set(0);
    }

    @Path("/selfRedirect/{selfRedirectsCount}")
    @POST
    public Response selfRedirect(@PathParam("selfRedirectsCount") int selfRedirectsCount) {
        int count = redirectCounter.incrementAndGet();

        final String loc;
        if (count <= selfRedirectsCount) {
            loc = "/RedirectRest/selfRedirect/" + selfRedirectsCount;
        } else {
            loc = "/soap/largeSlow";
        }
        Log.infof("redirectCounter at %d: sending 302 with Location %s", count, loc);
        return Response.status(302).header("Location", loc).build();
    }

    @Path("/loop1")
    @POST
    public Response loop1() {
        return Response.temporaryRedirect(URI.create("/RedirectRest/loop2")).build();
    }

    @Path("/loop2")
    @POST
    public Response loop2() {
        // Log.infof("/RedirectRest/singleRedirect Received payload %s", body);
        // Relative URI: return Response.status(302).header("Location", "/soap/largeSlow").build();
        // RestEASY will convert this to an absolute URI
        return Response.temporaryRedirect(URI.create("/RedirectRest/loop1")).build();
    }

    @Path("/async/{client}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> async(@PathParam("client") String client, @QueryParam("sizeBytes") int sizeBytes,
            @QueryParam("delayMs") int delayMs) {
        return CxfMutinyUtils
                .<LargeSlowResponse> toUni(handler -> getClient(client).largeSlowAsync(sizeBytes, delayMs, 0, handler))
                .map(response -> response.getReturn().getPayload());
    }

    @Path("/sync/{client}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String largeHelloSync(@PathParam("client") String client, @QueryParam("sizeBytes") int sizeBytes,
            @QueryParam("delayMs") int delayMs) {
        return getClient(client).largeSlow(sizeBytes, delayMs, 0).getPayload();
    }

    @Path("/retransmitCacheSync")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response retransmitCacheSync(
            String body,
            @HeaderParam(EXPECTED_FILE_COUNT_HEADER) int expectedFileCount,
            @HeaderParam(REQUEST_ID_HEADER) String requestId,
            @HeaderParam(STATUS_CODE_HEADER) String statusCode) {
        RetransmitCacheOutput result = null;
        Map<String, Object> reqContext = ((BindingProvider) retransmitCache).getRequestContext();
        reqContext.put(
                MessageContext.HTTP_REQUEST_HEADERS,
                statusCode != null && requestId != null
                        ? Map.of(
                                EXPECTED_FILE_COUNT_HEADER, List.of(String.valueOf(expectedFileCount)),
                                REQUEST_ID_HEADER, List.of(requestId),
                                STATUS_CODE_HEADER, List.of(statusCode))
                        : Map.of());
        try {
            result = retransmitCache.retransmitCache(expectedFileCount, body);
            return Response.ok(result.getPayload()).build();
        } catch (SOAPFaultException e) {
            Map<String, Object> responseContext = ((BindingProvider) retransmitCache).getResponseContext();
            final int sc = (Integer) responseContext.get(Message.RESPONSE_CODE);
            if (sc != 200) {
                return Response.status(sc).build();
            }
            return Response.status(500, "Unexpected").build();
        }
    }

    @Path("/retransmitCacheAsyncBlocking")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    //@Blocking
    public Uni<Response> retransmitCacheAsyncBlocking(
            String body,
            @HeaderParam(EXPECTED_FILE_COUNT_HEADER) int expectedFileCount,
            @HeaderParam(REQUEST_ID_HEADER) String requestId,
            @HeaderParam(STATUS_CODE_HEADER) String statusCode) {
        Map<String, Object> reqContext = ((BindingProvider) retransmitCache).getRequestContext();
        reqContext.put(
                MessageContext.HTTP_REQUEST_HEADERS,
                statusCode != null && requestId != null
                        ? Map.of(
                                EXPECTED_FILE_COUNT_HEADER, List.of(String.valueOf(expectedFileCount)),
                                REQUEST_ID_HEADER, List.of(requestId),
                                STATUS_CODE_HEADER, List.of(statusCode))
                        : Map.of());

        return CxfMutinyUtils.<io.quarkiverse.cxf.it.redirect.retransmitcache.RetransmitCacheResponse> toResponseUni(
                handler -> retransmitCache.retransmitCacheAsync(expectedFileCount, body, handler))
                .map(retransmitCacheResponse -> retransmitCacheResponse.getPayload().getReturn().getPayload())
                .map(payload -> Response.ok(payload).build())
                .onFailure().recoverWithItem(e -> {
                    if (e instanceof FailedResponse) {
                        final FailedResponse fr = (FailedResponse) e;
                        final int sc = (Integer) fr.getContext().get(Message.RESPONSE_CODE);
                        return Response.status(sc).build();
                    }
                    return Response.status(500).build();
                });
    }

    public static final String EXPECTED_FILE_COUNT_HEADER = "X-Expected-File-Count";
    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String STATUS_CODE_HEADER = "X-Status-Code";
    private final Map<String, String> tempFiles = new ConcurrentHashMap<>();

    @Path("/retransmitCacheRedirect")
    @POST
    public Response retransmitCacheRedirect(
            String body, // consume the body, otherwise the test fails
            @HeaderParam(EXPECTED_FILE_COUNT_HEADER) int expectedFileCount,
            @HeaderParam(REQUEST_ID_HEADER) String requestId,
            @HeaderParam(STATUS_CODE_HEADER) Integer statusCode) {

        if (statusCode != null && requestId != null) {
            Log.infof("Enforcing status %d", statusCode);
            Properties props = RetransmitCacheServiceImpl.listTempFiles(expectedFileCount, retransmitCacheDir);
            final String propsString = RetransmitCacheServiceImpl.toString(props);
            tempFiles.put(requestId, propsString);
            Log.infof("Paths %s", props.keySet());
            return Response.status(statusCode).build();
        }

        return Response.temporaryRedirect(URI.create("/soap/retransmitCache")).build();
    }

    @Path("/retransmitCache-tempFiles/{requestId}")
    @GET
    public String retransmitCacheTempFiles(@PathParam("requestId") String requestId) {
        return tempFiles.get(requestId);
    }

}
