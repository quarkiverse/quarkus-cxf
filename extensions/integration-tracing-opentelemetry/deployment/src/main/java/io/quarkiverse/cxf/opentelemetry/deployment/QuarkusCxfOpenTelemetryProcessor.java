package io.quarkiverse.cxf.opentelemetry.deployment;

import io.quarkiverse.cxf.opentelemetry.OpenTelemetryCustomizer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.opentelemetry.deployment.OpenTelemetryEnabled;

public class QuarkusCxfOpenTelemetryProcessor {

    private static final String FEATURE = "cxf-integration-tracing-opentelemetry";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep(onlyIf = OpenTelemetryEnabled.class)
    void additionalBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(
                new AdditionalBeanBuildItem(OpenTelemetryCustomizer.class));
    }

}
