package io.quarkiverse.cxf.ws.rm.deployment;

import java.util.stream.Stream;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;

public class WsReliableMessagingProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem("cxf-rt-ws-rm");
    }

    @BuildStep
    void indexDependencies(BuildProducer<IndexDependencyBuildItem> indexDependencies) {
        Stream.of(
                "org.apache.cxf:cxf-rt-ws-rm",
                "org.apache.cxf:cxf-rt-management")
                .forEach(ga -> {
                    String[] coords = ga.split(":");
                    indexDependencies.produce(new IndexDependencyBuildItem(coords[0], coords[1]));
                });
    }

    @BuildStep
    void runtimeInitializedClass(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClass) {
        Stream.of(
                "org.apache.cxf.ws.rm.ManagedRMEndpoint")
                .map(RuntimeInitializedClassBuildItem::new)
                .forEach(runtimeInitializedClass::produce);
    }

}
