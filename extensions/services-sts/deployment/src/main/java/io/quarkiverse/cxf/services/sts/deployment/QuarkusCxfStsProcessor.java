package io.quarkiverse.cxf.services.sts.deployment;

import java.util.stream.Stream;

import org.apache.cxf.sts.rest.RESTSecurityTokenService;
import org.apache.cxf.sts.rest.RESTSecurityTokenServiceImpl;
import org.jboss.jandex.IndexView;

import io.quarkus.arc.deployment.BuildTimeConditionBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;

public class QuarkusCxfStsProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem("cxf-services-sts");
    }

    @BuildStep
    void indexDependencies(BuildProducer<IndexDependencyBuildItem> indexDependencies) {
        Stream.of(
                "org.apache.cxf:cxf-rt-rs-security-jose",
                "org.apache.cxf:cxf-rt-rs-json-basic",
                "org.apache.cxf.services.sts:cxf-services-sts-core")
                .forEach(ga -> {
                    String[] coords = ga.split(":");
                    indexDependencies.produce(new IndexDependencyBuildItem(coords[0], coords[1]));
                });
    }

    @BuildStep
    void buildTimeConditions(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<BuildTimeConditionBuildItem> buildTimeConditions) {

        final IndexView index = combinedIndexBuildItem.getIndex();

        /*
         * RESTSecurityTokenService and RESTSecurityTokenServiceImpl have JAX-RS annotations
         * and thus quarkus-resteasy (if available in the class path) wants to deploy them
         * But they would not work because they are missing some Spring stuff that we exclude
         * So we rather hide them from RESTeasy
         */
        Stream.of(RESTSecurityTokenService.class, RESTSecurityTokenServiceImpl.class)
                .map(cl -> new BuildTimeConditionBuildItem(index.getClassByName(cl), false))
                .forEach(buildTimeConditions::produce);

    }

}
