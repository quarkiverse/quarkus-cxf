package io.quarkiverse.cxf.metrics;

import java.util.function.BiConsumer;

import io.quarkiverse.cxf.CXFClientInfo;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.http.HttpClientOptions;

/**
 * @since 2.25.0
 */
@Recorder
public class CxfMetricsRecorder {
    public RuntimeValue<BiConsumer<CXFClientInfo, HttpClientOptions>> httpClientCustomizer() {
        return new RuntimeValue<BiConsumer<CXFClientInfo, HttpClientOptions>>(
                (clientInfo, opts) -> {
                    opts.setMetricsName("cxf|" + clientInfo.getConfigKey());
                });
    }
}
