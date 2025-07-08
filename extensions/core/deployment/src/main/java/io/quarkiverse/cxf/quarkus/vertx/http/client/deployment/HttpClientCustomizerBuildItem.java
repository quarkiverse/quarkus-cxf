package io.quarkiverse.cxf.quarkus.vertx.http.client.deployment;

import java.util.function.BiConsumer;

import io.quarkiverse.cxf.CXFClientInfo;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.RuntimeValue;
import io.vertx.core.http.HttpClientOptions;

/**
 */
public final class HttpClientCustomizerBuildItem extends MultiBuildItem {

    private final RuntimeValue<BiConsumer<CXFClientInfo, HttpClientOptions>> customizer;

    public HttpClientCustomizerBuildItem(RuntimeValue<BiConsumer<CXFClientInfo, HttpClientOptions>> customizer) {
        super();
        this.customizer = customizer;
    }

    public RuntimeValue<BiConsumer<CXFClientInfo, HttpClientOptions>> getCustomizer() {
        return customizer;
    }
}
