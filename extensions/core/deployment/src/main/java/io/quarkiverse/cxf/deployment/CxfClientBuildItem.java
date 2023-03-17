package io.quarkiverse.cxf.deployment;

/**
 * Holds a client endpoint metadata.
 */
public final class CxfClientBuildItem extends AbstractEndpointBuildItem {
    private final String sei;
    private final boolean proxyClassRuntimeInitialized;

    public CxfClientBuildItem(String sei, String soapBinding, String wsNamespace,
            String wsName, boolean runtimeInitialized) {
        super(soapBinding, wsNamespace, wsName);
        this.sei = sei;
        this.proxyClassRuntimeInitialized = runtimeInitialized;
    }

    public String getSei() {
        return sei;
    }

    public boolean isProxyClassRuntimeInitialized() {
        return proxyClassRuntimeInitialized;
    }

}
