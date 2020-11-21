package io.quarkiverse.cxf.deployment;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.builder.item.MultiBuildItem;

public class CxfInfoBuildItem extends MultiBuildItem {
    private String sei;
    private String wsdlUrl;
    private List<String> inInterceptors;
    private List<String> outInterceptors;
    private List<String> outFaultInterceptors;
    private List<String> inFaultInterceptors;
    private List<String> features;

    public CxfInfoBuildItem(String sei, String wsdlUrl) {
        this.sei = sei;
        this.wsdlUrl = wsdlUrl;
        this.inInterceptors = new ArrayList<>();
        this.outInterceptors = new ArrayList<>();
        this.outFaultInterceptors = new ArrayList<>();
        this.inFaultInterceptors = new ArrayList<>();
        this.features = new ArrayList<>();
    }

    public String getSei() {
        return sei;
    }

    public String getWsdlUrl() {
        return wsdlUrl;
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
