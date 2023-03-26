package io.quarkiverse.cxf;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Provides runtime metadata for a CXF client.
 *
 * <p>
 * This class contains extracted from a SEI. It contains basic data to
 * setup a proxy client for a given SEI.
 * </p>
 */
public class CXFClientData implements Serializable {
    private Set<String> wrapperClassNames;
    private String soapBinding;
    private String sei;
    private String wsName;
    private String wsNamespace;
    private boolean proxyClassRuntimeInitialized;

    public CXFClientData() {
    }

    public CXFClientData(
            String soapBinding,
            String sei,
            String wsName,
            String wsNamespace,
            Collection<String> wrapperClassNames,
            boolean proxyClassRuntimeInitialized) {
        this.soapBinding = soapBinding;
        this.sei = sei;
        this.wsName = wsName;
        this.wsNamespace = wsNamespace;
        this.wrapperClassNames = new LinkedHashSet<>(wrapperClassNames);
        this.proxyClassRuntimeInitialized = proxyClassRuntimeInitialized;
    }

    public Set<String> getWrapperClassNames() {
        return wrapperClassNames;
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

    public void setProxyClassRuntimeInitialized(boolean proxyClassRuntimeInitialized) {
        this.proxyClassRuntimeInitialized = proxyClassRuntimeInitialized;
    }

    public void setWrapperClassNames(Set<String> classNames) {
        this.wrapperClassNames = classNames;
    }

    public void setSoapBinding(String soapBinding) {
        this.soapBinding = soapBinding;
    }

    public void setSei(String sei) {
        this.sei = sei;
    }

    public void setWsName(String wsName) {
        this.wsName = wsName;
    }

    public void setWsNamespace(String wsNamespace) {
        this.wsNamespace = wsNamespace;
    }
}
