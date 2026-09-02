package io.quarkiverse.cxf.metrics.client.it;

import java.util.StringJoiner;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.eap.quickstarts.wscalculator.calculator.AddResponse;
import org.jboss.eap.quickstarts.wscalculator.calculator.CalculatorService;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.MeterNotFoundException;
import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkiverse.cxf.mutiny.CxfMutinyUtils;
import io.quarkiverse.cxf.vertx.http.client.VertxHttpClientHTTPConduit.TimeoutIOException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@Path("/laggy-calculator")
public class LaggyCalculatorClientResource {

    @CXFClient("laggyCalculatorAsync")
    CalculatorService laggyCalculatorAsync;

    @CXFClient("laggyCalculatorSync")
    CalculatorService laggyCalculatorSync;

    @Inject
    MeterRegistry registry;

    @GET
    @Path("/call-clients-sync/{iterationCount}/{a}")
    @Produces(MediaType.TEXT_PLAIN)
    public String callClientsSync(@PathParam("iterationCount") int iterationCount, @PathParam("a") int a) {
        StringJoiner result = new StringJoiner("|");
        for (int j = 0; j < iterationCount; j++) {
            result.add(addSync(laggyCalculatorSync, a));
        }
        return result.toString();
    }

    private static String addSync(CalculatorService laggyCalculator, int a) {
        try {
            laggyCalculator.add(a, a);
            return "success";
        } catch (Exception e) {
            Throwable root = rootCause(e);
            if (root instanceof TimeoutIOException && root.getMessage().contains("ms to receive response headers")) {
                return "receive-timeout-headers";
            }
            return root.getMessage();
        }
    }

    @GET
    @Path("/call-clients-async/{iterationCount}/{a}")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> callClientsAsync(@PathParam("iterationCount") int iterationCount, @PathParam("a") int a) {
        return Multi.createFrom().range(0, iterationCount)
                .onItem().transformToUni(i -> addAsync(laggyCalculatorAsync, a))
                .merge()
                .collect().with(Collectors.joining("|"));
    }

    static Uni<String> addAsync(CalculatorService laggyCalculator, int a) {
        return CxfMutinyUtils.<AddResponse> toUni(handler -> laggyCalculator.addAsync(a, a, handler))
                .map(response -> "success")
                .onFailure()
                .recoverWithItem(t -> {
                    Throwable root = rootCause(t);
                    if (root instanceof TimeoutIOException && root.getMessage().contains("ms to receive response headers")) {
                        return "receive-timeout-headers";
                    }
                    return root.getMessage();
                });
    }

    @GET
    @Path("/pool-metrics/{clientKey}/{meterNotFoundAllowed}")
    @Produces(MediaType.TEXT_PLAIN)
    public String poolMetrics(@PathParam("clientKey") String clientKey,
            @PathParam("meterNotFoundAllowed") boolean meterNotFoundAllowed) {

        int activeTasks;
        try {
            activeTasks = registry.get("http.client.connections")
                    .tag("clientName", clientKey)
                    .longTaskTimer()
                    .activeTasks();
        } catch (MeterNotFoundException e) {
            activeTasks = meterNotFoundAllowed ? 0 : -1;
        } catch (Exception e) {
            activeTasks = -1;
            e.printStackTrace();
        }
        int queueSize;
        try {
            queueSize = (int) registry.get("http.client.queue.size")
                    .tag("clientName", clientKey)
                    .gauge()
                    .value();
        } catch (MeterNotFoundException e) {
            queueSize = meterNotFoundAllowed ? 0 : -1;
        } catch (Exception e) {
            queueSize = -1;
            e.printStackTrace();
        }
        return activeTasks + "," + queueSize;
    }

    static Throwable rootCause(Throwable t) {
        while (t.getCause() != null) {
            t = t.getCause();
        }
        return t;
    }
}
