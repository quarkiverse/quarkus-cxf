package io.quarkiverse.cxf.features.logging.deployment;

import java.util.stream.Stream;

import org.apache.cxf.ext.logging.LoggingFeature;

import io.quarkiverse.cxf.features.logging.LoggingFactoryCustomizer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;

public class QuarkusCxfLoggingProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem("cxf-rt-features-logging");
    }

    @BuildStep
    void indexDependencies(BuildProducer<IndexDependencyBuildItem> indexDependencies) {
        Stream.of(
                "org.apache.cxf:cxf-rt-features-logging")
                .forEach(ga -> {
                    String[] coords = ga.split(":");
                    indexDependencies.produce(new IndexDependencyBuildItem(coords[0], coords[1]));
                });
    }

    @BuildStep
    void unremovableBean(BuildProducer<UnremovableBeanBuildItem> unremovable) {
        unremovable.produce(UnremovableBeanBuildItem.beanTypes(LoggingFeature.class));
    }

    @BuildStep
    void additionalBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(new AdditionalBeanBuildItem(LoggingFactoryCustomizer.class));
    }

}
