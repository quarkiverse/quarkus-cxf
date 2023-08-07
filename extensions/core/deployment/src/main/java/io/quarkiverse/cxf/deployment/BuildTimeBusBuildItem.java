package io.quarkiverse.cxf.deployment;

import org.apache.cxf.Bus;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Holds the build time {@link Bus} instance.
 */
public final class BuildTimeBusBuildItem extends SimpleBuildItem {
    private final Bus bus;

    public Bus getBus() {
        return bus;
    }

    public BuildTimeBusBuildItem(Bus bus) {
        super();
        this.bus = bus;
    }

}