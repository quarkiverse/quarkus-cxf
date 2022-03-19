package io.quarkiverse.cxf;

import java.util.ArrayList;
import java.util.List;

import org.jboss.logging.Logger;

public class CXFServletInfo {
    private final String relativePath;
    private final String path;
    private final String className;
    private final List<String> inInterceptors;
    private final List<String> outInterceptors;
    private final List<String> outFaultInterceptors;
    private final List<String> inFaultInterceptors;
    private final List<String> features;
    private final List<String> handlers;
    private final String sei;
    private final String wsdlPath;
    private final String soapBinding;
    private final List<String> wrapperClassNames;
    private final String endpointUrl;

    private static final Logger LOGGER = Logger.getLogger(CXFServletInfo.class);

    public CXFServletInfo(String path, String relativePath, String className, String sei, String wsdlPath, String soapBinding,
            List<String> wrapperClassNames, String endpointUrl) {
        LOGGER.trace("new CXFServletInfo");
        this.path = path;
        this.relativePath = relativePath;
        this.className = className;
        this.inInterceptors = new ArrayList<>();
        this.outInterceptors = new ArrayList<>();
        this.outFaultInterceptors = new ArrayList<>();
        this.inFaultInterceptors = new ArrayList<>();
        this.features = new ArrayList<>();
        this.handlers = new ArrayList<>();
        this.sei = sei;
        this.wsdlPath = wsdlPath;
        this.soapBinding = soapBinding;
        this.wrapperClassNames = wrapperClassNames;
        this.endpointUrl = endpointUrl;
    }

    public String getClassName() {
        return className;
    }

    public String getPath() {
        return path;
    }

    public String getRelativePath() {
        return relativePath;
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

    public String getSOAPBinding() {
        return soapBinding;
    }

    public List<String> getWrapperClassNames() {
        return wrapperClassNames;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    @Override
    public String toString() {
        return "Web Service " + className + " on " + path;
    }
}
