package io.quarkiverse.cxf;

import java.io.Serializable;

/**
 * That specific part of CXF client metadata that is known at build time.
 */
public class CXFClientData implements Serializable {
    private static final long serialVersionUID = 1L;
    final private String soapBinding;
    final private String sei;
    final private String wsName;
    final private String wsNamespace;
    final private boolean proxyClassRuntimeInitialized;

    public CXFClientData(
            String soapBinding,
            String sei,
            String wsName,
            String wsNamespace,
            boolean proxyClassRuntimeInitialized) {
        this.soapBinding = soapBinding;
        this.sei = sei;
        this.wsName = wsName;
        this.wsNamespace = wsNamespace;
        this.proxyClassRuntimeInitialized = proxyClassRuntimeInitialized;
    }

    public String getSoapBinding() {
        return soapBinding;
    }

    public String getSei() {
        return sei;
    }

    public String getWsName() {
        return wsName;
    }

    public String getWsNamespace() {
        return wsNamespace;
    }

    public boolean isProxyClassRuntimeInitialized() {
        return proxyClassRuntimeInitialized;
    }
}
