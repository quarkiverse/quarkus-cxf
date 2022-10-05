package io.quarkiverse.cxf.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 */
abstract class AbstractEndpointBuildItem extends MultiBuildItem {
    private final String soapBinding;
    private final String wsNamespace;
    private final String wsName;

    AbstractEndpointBuildItem(String soapBinding, String wsNamespace, String wsName) {
        super();
        this.soapBinding = soapBinding;
        this.wsNamespace = wsNamespace;
        this.wsName = wsName;
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
