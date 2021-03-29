package io.quarkiverse.cxf;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

import io.quarkus.arc.Unremovable;

@Unremovable
public class CXFClientInfo {
    private static final Logger LOGGER = Logger.getLogger(CXFClientInfo.class);
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
    private final List<String> inInterceptors = new ArrayList<>();
    private final List<String> outInterceptors = new ArrayList<>();
    private final List<String> outFaultInterceptors = new ArrayList<>();
    private final List<String> inFaultInterceptors = new ArrayList<>();
    private final List<String> features = new ArrayList<>();
    private final List<String> classNames = new ArrayList<>();

    public CXFClientInfo() {
    }

    public CXFClientInfo(
            String sei,
            String endpointAddress,
            String soapBinding,
            String wsNamespace,
            String wsName,
            List<String> classNames) {
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
    }

    public CXFClientInfo(CXFClientInfo other) {
        this(other.sei, other.endpointAddress, other.soapBinding, other.wsNamespace, other.wsName, other.classNames);
        this.wsdlUrl = other.wsdlUrl;
        this.epNamespace = other.epNamespace;
        this.epName = other.epName;
        this.username = other.username;
        this.password = other.password;
        this.features.addAll(other.features);
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
        this.endpointAddress = config.clientEndpointUrl.orElse(this.endpointAddress);
        addFeatures(config);
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

    @Override
    public String toString() {
        return "CXFClientInfo{" +
                "sei='" + sei + '\'' +
                ", endpointAddress='" + endpointAddress + '\'' +
                ", wsdlUrl='" + wsdlUrl + '\'' +
                ", soapBinding='" + soapBinding + '\'' +
                ", wsNamespace='" + wsNamespace + '\'' +
                ", wsName='" + wsName + '\'' +
                ", epNamespace='" + epNamespace + '\'' +
                ", epName='" + epName + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", inInterceptors=" + inInterceptors +
                ", outInterceptors=" + outInterceptors +
                ", outFaultInterceptors=" + outFaultInterceptors +
                ", inFaultInterceptors=" + inFaultInterceptors +
                ", features=" + features +
                ", classNames=" + classNames +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        CXFClientInfo that = (CXFClientInfo) o;
        return Objects.equals(sei, that.sei) && Objects.equals(
                endpointAddress,
                that.endpointAddress) && Objects.equals(wsdlUrl, that.wsdlUrl)
                && Objects.equals(
                        soapBinding,
                        that.soapBinding)
                && Objects.equals(wsNamespace, that.wsNamespace) && Objects.equals(
                        wsName,
                        that.wsName)
                && Objects.equals(epNamespace, that.epNamespace) && Objects.equals(
                        epName,
                        that.epName)
                && Objects.equals(username, that.username) && Objects.equals(
                        password,
                        that.password)
                && Objects.equals(inInterceptors, that.inInterceptors) && Objects.equals(
                        outInterceptors,
                        that.outInterceptors)
                && Objects.equals(outFaultInterceptors, that.outFaultInterceptors) && Objects.equals(
                        inFaultInterceptors,
                        that.inFaultInterceptors)
                && Objects.equals(features, that.features) && Objects.equals(classNames, that.classNames);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                sei,
                endpointAddress,
                wsdlUrl,
                soapBinding,
                wsNamespace,
                wsName,
                epNamespace,
                epName,
                username,
                password,
                inInterceptors,
                outInterceptors,
                outFaultInterceptors,
                inFaultInterceptors,
                features,
                classNames);
    }
}
