package io.quarkiverse.cxf;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.quarkus.arc.Unremovable;

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
    private boolean proxyClassRuntimeInitialized;
    private final List<String> inInterceptors = new ArrayList<>();
    private final List<String> outInterceptors = new ArrayList<>();
    private final List<String> outFaultInterceptors = new ArrayList<>();
    private final List<String> inFaultInterceptors = new ArrayList<>();
    private final List<String> features = new ArrayList<>();
    private final List<String> handlers = new ArrayList<>();
    private final List<String> classNames = new ArrayList<>();

    public CXFClientInfo() {
    }

    public CXFClientInfo(
            String sei,
            String endpointAddress,
            String soapBinding,
            String wsNamespace,
            String wsName,
            List<String> classNames,
            boolean proxyClassRuntimeInitialized) {
        this.classNames.addAll(classNames);
        this.endpointAddress = endpointAddress;
        this.epName = null;
        this.epNamespace = null;
        this.password = null;
        this.sei = sei;
        this.soapBinding = soapBinding;
        this.username = null;
        this.wsName = wsName;
        this.wsNamespace = wsNamespace;
        this.wsdlUrl = null;
        this.proxyClassRuntimeInitialized = proxyClassRuntimeInitialized;
    }

    public CXFClientInfo(CXFClientInfo other) {
        this(other.sei, other.endpointAddress, other.soapBinding, other.wsNamespace, other.wsName, other.classNames,
                other.proxyClassRuntimeInitialized);
        this.wsdlUrl = other.wsdlUrl;
        this.epNamespace = other.epNamespace;
        this.epName = other.epName;
        this.username = other.username;
        this.password = other.password;
        this.features.addAll(other.features);
        this.handlers.addAll(other.handlers);
        this.inFaultInterceptors.addAll(other.inFaultInterceptors);
        this.inInterceptors.addAll(other.inInterceptors);
        this.outFaultInterceptors.addAll(other.outFaultInterceptors);
        this.outInterceptors.addAll(other.outInterceptors);
    }

    public CXFClientInfo withConfig(CxfClientConfig config) {
        Objects.requireNonNull(config);
        this.wsdlUrl = config.wsdlPath.orElse(this.wsdlUrl);
        this.epNamespace = config.endpointNamespace.orElse(this.epNamespace);
        this.epName = config.endpointName.orElse(this.epName);
        this.username = config.username.orElse(this.username);
        this.password = config.password.orElse(this.password);
        this.soapBinding = config.soapBinding.orElse(this.soapBinding);
        this.endpointAddress = config.clientEndpointUrl.orElse(this.endpointAddress);
        addFeatures(config);
        addHandlers(config);
        addInterceptors(config);
        return this;
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

    public boolean isProxyClassRuntimeInitialized() {
        return proxyClassRuntimeInitialized;
    }

    public List<String> getFeatures() {
        return features;
    }

    public List<String> getHandlers() {
        return handlers;
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

    private CXFClientInfo addInterceptors(CxfClientConfig cxfEndPointConfig) {
        if (cxfEndPointConfig.inInterceptors.isPresent()) {
            this.inInterceptors.addAll(cxfEndPointConfig.inInterceptors.get());
        }
        if (cxfEndPointConfig.outInterceptors.isPresent()) {
            this.outInterceptors.addAll(cxfEndPointConfig.outInterceptors.get());
        }
        if (cxfEndPointConfig.outFaultInterceptors.isPresent()) {
            this.outFaultInterceptors.addAll(cxfEndPointConfig.outFaultInterceptors.get());
        }
        if (cxfEndPointConfig.inFaultInterceptors.isPresent()) {
            this.inFaultInterceptors.addAll(cxfEndPointConfig.inFaultInterceptors.get());
        }
        return this;
    }

    private CXFClientInfo addFeatures(CxfClientConfig cxfEndPointConfig) {
        if (cxfEndPointConfig.features.isPresent()) {
            this.features.addAll(cxfEndPointConfig.features.get());
        }
        return this;
    }

    private CXFClientInfo addHandlers(CxfClientConfig cxfEndPointConfig) {
        if (cxfEndPointConfig.handlers.isPresent()) {
            this.handlers.addAll(cxfEndPointConfig.handlers.get());
        }
        return this;
    }
}
