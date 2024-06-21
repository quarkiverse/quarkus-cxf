package io.quarkiverse.cxf.deployment;

import java.util.List;

import io.quarkus.deployment.builditem.FeatureBuildItem;

public enum QuarkusCxfFeature {
    CXF("cxf"),
    CXF_RT_TRANSPORTS_HTTP_HC5("cxf-rt-transports-http-hc5");

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

    public static boolean hc5Present(List<FeatureBuildItem> features) {
        return features.stream()
                .map(FeatureBuildItem::getName)
                .anyMatch(feature -> feature.equals(CXF_RT_TRANSPORTS_HTTP_HC5.getKey()));
    }

}
