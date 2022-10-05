package io.quarkiverse.cxf.deployment;

/**
 * Holds a client endpoint metadata.
 */
public final class CxfClientBuildItem extends AbstractEndpointBuildItem {
    public CxfClientBuildItem(String sei, String soapBinding, String wsNamespace,
            String wsName) {
        super(soapBinding, wsNamespace, wsName);
        this.sei = sei;
    }

    private final String sei;

    public String getSei() {
        return sei;
    }
}
