package io.quarkiverse.cxf.deployment;

import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;

/**
 */
abstract class AbstractEndpointBuildItem extends MultiBuildItem {
    private final String soapBinding;
    private final String wsNamespace;
    private final String wsName;

    AbstractEndpointBuildItem(String soapBinding, String wsNamespace, String wsName) {
        super();
        this.soapBinding = Objects.requireNonNull(soapBinding, "soapBinding cannot be null");
        this.wsNamespace = Objects.requireNonNull(wsNamespace, "wsNamespace cannot be null");
        this.wsName = Objects.requireNonNull(wsName, "wsName cannot be null");
    }

    public String getSoapBinding() {
        return soapBinding;
    }

    public String getWsName() {
        return wsName;
    }

    public String getWsNamespace() {
        return wsNamespace;
    }
}
