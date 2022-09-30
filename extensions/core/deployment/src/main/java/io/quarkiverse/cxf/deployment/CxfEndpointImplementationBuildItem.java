package io.quarkiverse.cxf.deployment;

/**
 * Holds service endpoint implementation metadata.
 */
public final class CxfEndpointImplementationBuildItem extends AbstractEndpointBuildItem {

    private final String implementor;
    private final boolean provider;

    public CxfEndpointImplementationBuildItem(String path, String implementor, String soapBinding, String wsNamespace,
            String wsName, boolean provider) {
        super(soapBinding, wsNamespace, wsName, path);
        this.implementor = implementor;
        this.provider = provider;
    }

    public String getImplementor() {
        return implementor;
    }

    public boolean isProvider() {
        return provider;
    }
}
