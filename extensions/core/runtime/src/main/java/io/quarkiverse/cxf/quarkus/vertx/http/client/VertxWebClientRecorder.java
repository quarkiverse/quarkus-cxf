package io.quarkiverse.cxf.quarkus.vertx.http.client;

import java.util.function.Consumer;

import org.apache.cxf.Bus;

import io.quarkiverse.cxf.vertx.http.client.HttpClientPool;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class VertxWebClientRecorder {

    public RuntimeValue<Consumer<Bus>> customizeBus() {

        return new RuntimeValue<>(bus -> {
            InstanceHandle<HttpClientPool> httpClientPool = Arc.container().instance(HttpClientPool.class);
            if (httpClientPool.isAvailable()) {
                bus.setExtension(httpClientPool.get(), HttpClientPool.class);
            } else {
                throw new IllegalStateException(HttpClientPool.class.getName() + " not available in Arc");
            }
        });
    }
}
