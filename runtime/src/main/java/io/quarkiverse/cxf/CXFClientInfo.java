package io.quarkiverse.cxf;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

import io.quarkus.arc.Unremovable;

@ApplicationScoped
@Unremovable
public class CXFClientInfo {
    private String sei;
    private final String endpointAddress;
    private final String wsdlUrl;
    private final String soapBinding;
    private final String wsNamespace;
    private final String wsName;
    private final String epNamespace;
    private final String epName;
    private final String username;
    private final String password;
    private final List<String> inInterceptors;
    private final List<String> outInterceptors;
    private final List<String> outFaultInterceptors;
    private final List<String> inFaultInterceptors;
    private final List<String> features;
    private final List<String> classNames;
    private static final Logger LOGGER = Logger.getLogger(CXFClientInfo.class);

    public CXFClientInfo(String sei, String endpointAddress, String wsdlUrl, String soapBinding, String wsNamespace,
            String wsName, String epNamespace, String epName, String username, String password, List<String> classNames) {
        LOGGER.warn("new CXFClientInfo");
        this.sei = sei;
        this.endpointAddress = endpointAddress;
        this.wsdlUrl = wsdlUrl;
        this.soapBinding = soapBinding;
        this.wsNamespace = wsNamespace;
        this.wsName = wsName;
        this.epNamespace = epNamespace;
        this.epName = epName;
        this.classNames = classNames;
        this.username = username;
        this.password = password;
        this.inInterceptors = new ArrayList<>();
        this.outInterceptors = new ArrayList<>();
        this.outFaultInterceptors = new ArrayList<>();
        this.inFaultInterceptors = new ArrayList<>();
        this.features = new ArrayList<>();
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

    public List<String> getClassNames() {
        return classNames;
    }

    public List<String> getFeatures() {
        return features;
    }

    public List<String> getInInterceptors() {
        return inInterceptors;
    }

    public List<String> getOutInterceptors() {
        return outInterceptors;
    }

    public List<String> getOutFaultInterceptors() {
        return outFaultInterceptors;
    }

    public List<String> getInFaultInterceptors() {
        return inFaultInterceptors;
    }
}
