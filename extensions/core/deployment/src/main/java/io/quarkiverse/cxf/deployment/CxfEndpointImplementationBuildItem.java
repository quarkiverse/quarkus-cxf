package io.quarkiverse.cxf.deployment;

import java.util.Objects;

/**
 * Holds service endpoint implementation metadata.
 */
public final class CxfEndpointImplementationBuildItem extends AbstractEndpointBuildItem {

    private final String implementor;
    private final boolean provider;

    public CxfEndpointImplementationBuildItem(String implementor, String soapBinding, String wsNamespace,
            String wsName, boolean provider) {
        super(soapBinding, wsNamespace, wsName);
        this.implementor = Objects.requireNonNull(implementor, "implementor cannot be null");
        this.provider = provider;
    }

    public String getImplementor() {
        return implementor;
    }

    public boolean isProvider() {
        return provider;
    }
}
