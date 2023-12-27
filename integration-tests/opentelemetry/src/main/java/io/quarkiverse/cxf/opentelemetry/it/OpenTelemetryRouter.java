package io.quarkiverse.cxf.opentelemetry.it;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkiverse.cxf.annotation.CXFClient;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

@ApplicationScoped
@RegisterForReflection(classNames = { "io.opentelemetry.sdk.trace.data.SpanData" }, registerFullHierarchy = true)
public class OpenTelemetryRouter {
    @Inject
    Router router;

    @Inject
    @CXFClient("hello")
    HelloService helloClient;

    @Inject
    InMemorySpanExporter exporter;

    public void register(@Observes StartupEvent ev) {
        router.post("/opentelemetry/client/hello")
                .handler(BodyHandler.create())
                .blockingHandler(rc -> rc.response()
                        .putHeader("content-type", "text/plain; charset=utf-8")
                        .end(helloClient.hello(rc.body().asString())));

        router.get("/opentelemetry/reset").handler(rc -> {
            exporter.reset();
            rc.response().end();
        });

        router.get("/opentelemetry/export").handler(rc -> {
            List<SpanData> export = exporter.getFinishedSpanItems()
                    .stream()
                    .filter(sd -> !sd.getName().contains("export")
                            && !sd.getName().contains("reset")
            //&& !sd.getName().equals("POST /opentelemetry/client/hello")
            )
                    .collect(Collectors.toList());

            rc.response()
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(Json.encodePrettily(export));
        });
    }

    @ApplicationScoped
    static class InMemorySpanExporterProducer {
        @Produces
        @Singleton
        InMemorySpanExporter inMemorySpanExporter() {
            return InMemorySpanExporter.create();
        }
    }

}
