package io.quarkiverse.cxf;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

import io.quarkus.arc.Unremovable;

@ApplicationScoped
@Unremovable
public class CXFServletInfo {
    private String path;
    private String className;
    private List<String> inInterceptors;
    private List<String> outInterceptors;
    private List<String> outFaultInterceptors;
    private List<String> inFaultInterceptors;
    private List<String> features;
    private String sei;
    private String wsdlPath;
    private String soapBinding;
    private List<String> wrapperClassNames;

    private static final Logger LOGGER = Logger.getLogger(CXFServletInfo.class);

    public CXFServletInfo(String path, String className, String sei, String wsdlPath, String soapBinding,
            List<String> wrapperClassNames) {
        super();
        LOGGER.warn("new CXFServletInfo");
        this.path = path;
        this.className = className;
        this.inInterceptors = new ArrayList<>();
        this.outInterceptors = new ArrayList<>();
        this.outFaultInterceptors = new ArrayList<>();
        this.inFaultInterceptors = new ArrayList<>();
        this.features = new ArrayList<>();
        this.sei = sei;
        this.wsdlPath = wsdlPath;
        this.soapBinding = soapBinding;
        this.wrapperClassNames = wrapperClassNames;
    }

    public String getClassName() {
        return className;
    }

    public String getPath() {
        return path;
    }

    public String getWsdlPath() {
        return wsdlPath;
    }

    public String getSei() {
        return sei;
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

    public String getSOAPBinding() {
        return soapBinding;
    }

    public List<String> getWrapperClassNames() {
        return wrapperClassNames;
    }

    @Override
    public String toString() {
        return "Web Service " + className + " on " + path;
    }
}
