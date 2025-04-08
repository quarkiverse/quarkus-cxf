package io.quarkiverse.cxf.deployment;

import io.quarkus.deployment.builditem.FeatureBuildItem;

public enum QuarkusCxfFeature {
    CXF("cxf");

    private final String key;

    private QuarkusCxfFeature(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public FeatureBuildItem asFeature() {
        return new FeatureBuildItem(this.key);
    }

}
