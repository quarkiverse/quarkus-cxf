package io.quarkiverse.cxf.features.clustering.deployment;

import java.util.stream.Stream;

import org.apache.cxf.clustering.FailoverFeature;
import org.apache.cxf.clustering.LoadDistributorFeature;
import org.apache.cxf.clustering.circuitbreaker.CircuitBreakerFailoverFeature;

import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;

public class QuarkusCxfClusteringProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem("cxf-rt-features-clustering");
    }

    @BuildStep
    void indexDependencies(BuildProducer<IndexDependencyBuildItem> indexDependencies) {
        Stream.of(
                "org.apache.cxf:cxf-rt-features-clustering")
                .forEach(ga -> {
                    String[] coords = ga.split(":");
                    indexDependencies.produce(new IndexDependencyBuildItem(coords[0], coords[1]));
                });
    }

    @BuildStep
    void unremovableBean(BuildProducer<UnremovableBeanBuildItem> unremovable) {
        unremovable.produce(UnremovableBeanBuildItem.beanTypes(FailoverFeature.class,
                LoadDistributorFeature.class, CircuitBreakerFailoverFeature.class));
    }

}
