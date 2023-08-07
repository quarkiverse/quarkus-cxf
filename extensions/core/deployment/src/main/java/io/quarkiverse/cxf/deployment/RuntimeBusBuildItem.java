package io.quarkiverse.cxf.deployment;

import org.apache.cxf.Bus;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;

/**
 * Holds the runtime {@link Bus} reference.
 */
public final class RuntimeBusBuildItem extends SimpleBuildItem {
    private final RuntimeValue<Bus> bus;

    public RuntimeValue<Bus> getBus() {
        return bus;
    }

    public RuntimeBusBuildItem(RuntimeValue<Bus> bus) {
        super();
        this.bus = bus;
    }

}