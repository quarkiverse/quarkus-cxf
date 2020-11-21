package io.quarkiverse.cxf.deployment;

import java.util.List;

public final class CxfClientBuildItem extends CxfInfoBuildItem {

    private String endpointAddress;
    private String soapBinding;
    private String wsNamespace;
    private String wsName;
    private String epNamespace;
    private String epName;
    private String username;
    private String password;
    private List<String> classNames;

    public CxfClientBuildItem(String sei, String endpointAddress, String wsdlUrl, String soapBinding, String wsNamespace,
            String wsName, String epNamespace, String epName, String username, String password, List<String> classNames) {
        super(sei, wsdlUrl);
        this.endpointAddress = endpointAddress;
        this.soapBinding = soapBinding;
        this.wsNamespace = wsNamespace;
        this.wsName = wsName;
        this.epNamespace = epNamespace;
        this.epName = epName;
        this.classNames = classNames;
        this.username = username;
        this.password = password;
    }

    public String getEndpointAddress() {
        return endpointAddress;
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

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public List<String> getClassNames() {
        return classNames;
    }
}
