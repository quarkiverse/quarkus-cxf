package io.quarkiverse.cxf.deployment;

import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Holds a fully qualified class name of a service implementation or of a service interface for purposes of ancillary
 * class generation at build time.
 */
public final class ServiceSeiBuildItem extends MultiBuildItem {
    private final String sei;

    public ServiceSeiBuildItem(String sei) {
        super();
        this.sei = Objects.requireNonNull(sei, "sei cannot be null");
    }

    public String getSei() {
        return sei;
    }
}
