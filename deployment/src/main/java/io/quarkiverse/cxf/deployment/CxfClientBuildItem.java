package io.quarkiverse.cxf.deployment;

import java.util.List;

import io.quarkus.builder.item.MultiBuildItem;

public final class CxfClientBuildItem extends MultiBuildItem {
    private String sei;
    private String endpointAddress;
    private String wsdlUrl;
    private String soapBinding;
    private String wsNamespace;
    private String wsName;
    private String epNamespace;
    private String epName;
    private List<String> classNames;

    public CxfClientBuildItem(String sei, String endpointAddress, String wsdlUrl, String soapBinding, String wsNamespace,
            String wsName, String epNamespace, String epName, List<String> classNames) {
        this.sei = sei;
        this.endpointAddress = endpointAddress;
        this.wsdlUrl = wsdlUrl;
        this.soapBinding = soapBinding;
        this.wsNamespace = wsNamespace;
        this.wsName = wsName;
        this.epNamespace = epNamespace;
        this.epName = epName;
        this.classNames = classNames;
    }

    public String getSei() {
        return sei;
    }

    public void setSei(String sei) {
        this.sei = sei;
    }

    public String getEndpointAddress() {
        return endpointAddress;
    }

    public String getWsdlUrl() {
        return wsdlUrl;
    }

    public String getSoapBinding() {
        return soapBinding;
    }

    public String getWsNamespace() {
        return wsNamespace;
    }

    public String getWsName() {
        return wsName;
    }

    public String getEpNamespace() {
        return epNamespace;
    }

    public String getEpName() {
        return epName;
    }

    public List<String> getClassNames() {
        return classNames;
    }
}
