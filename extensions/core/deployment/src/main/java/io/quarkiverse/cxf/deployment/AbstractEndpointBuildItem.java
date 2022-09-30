package io.quarkiverse.cxf.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 */
abstract class AbstractEndpointBuildItem extends MultiBuildItem {
    private final String soapBinding;
    private final String wsNamespace;
    private final String wsName;
    private final String path;

    AbstractEndpointBuildItem(String soapBinding, String wsNamespace, String wsName, String path) {
        super();
        this.soapBinding = soapBinding;
        this.wsNamespace = wsNamespace;
        this.wsName = wsName;
        this.path = path;
    }

    public String getPath() {
        return path;
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
