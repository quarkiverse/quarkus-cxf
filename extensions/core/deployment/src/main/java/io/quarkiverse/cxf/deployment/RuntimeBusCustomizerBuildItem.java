package io.quarkiverse.cxf.deployment;

import java.util.function.Consumer;

import org.apache.cxf.Bus;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.RuntimeValue;

/**
 * Holds a {@link Consumer} that will be used to customize the runtime {@link Bus} right after its creation.
 */
public final class RuntimeBusCustomizerBuildItem extends MultiBuildItem {
    private final RuntimeValue<Consumer<Bus>> customizer;

    public RuntimeBusCustomizerBuildItem(RuntimeValue<Consumer<Bus>> customizer) {
        super();
        this.customizer = customizer;
    }

    public RuntimeValue<Consumer<Bus>> getCustomizer() {
        return customizer;
    }

}