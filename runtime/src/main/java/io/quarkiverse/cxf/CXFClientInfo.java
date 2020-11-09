package io.quarkiverse.cxf;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

import io.quarkus.arc.Unremovable;

@ApplicationScoped
@Unremovable
public class CXFClientInfo {
    private String sei;
    private String endpointAddress;
    private String wsdlUrl;
    private String soapBinding;
    private String wsNamespace;
    private String wsName;
    private String epNamespace;
    private String epName;
    private String username;
    private String password;
    private List<String> classeNames;
    private static final Logger LOGGER = Logger.getLogger(CXFClientInfo.class);

    public CXFClientInfo(String sei, String endpointAddress, String wsdlUrl, String soapBinding, String wsNamespace,
            String wsName, String epNamespace, String epName, String username, String password, List<String> classeNames) {
        LOGGER.warn("new CXFClientInfo");
        this.sei = sei;
        this.endpointAddress = endpointAddress;
        this.wsdlUrl = wsdlUrl;
        this.soapBinding = soapBinding;
        this.wsNamespace = wsNamespace;
        this.wsName = wsName;
        this.epNamespace = epNamespace;
        this.epName = epName;
        this.classeNames = classeNames;
        this.username = username;
        this.password = password;
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

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public List<String> getClasseNames() {
        return classeNames;
    }
}
