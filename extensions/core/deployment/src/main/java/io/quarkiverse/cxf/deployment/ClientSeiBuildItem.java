package io.quarkiverse.cxf.deployment;

import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Holds a fully qualified class name of a service interface used by a client for purposes of ancillary
 * class generation at build time.
 */
public final class ClientSeiBuildItem extends MultiBuildItem {
    private final String sei;

    public ClientSeiBuildItem(String sei) {
        super();
        this.sei = Objects.requireNonNull(sei, "sei cannot be null");
    }

    public String getSei() {
        return sei;
    }
}
