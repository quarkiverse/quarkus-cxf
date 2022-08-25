package io.quarkiverse.cxf.deployment;

import java.util.stream.Stream;

import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;

/**
 * {@link BuildStep}s related to {@code org.apache.ws.security:wss4j}
 */
public class Wss4jProcessor {
    @BuildStep
    void indexDependencies(BuildProducer<IndexDependencyBuildItem> indexDependencies) {
        Stream.of("org.apache.wss4j:wss4j-ws-security-dom")
                .forEach(ga -> {
                    String[] coords = ga.split(":");
                    indexDependencies.produce(new IndexDependencyBuildItem(coords[0], coords[1]));
                });
    }

    @BuildStep
    void reflectiveClass(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        final IndexView index = combinedIndexBuildItem.getIndex();
        Stream.of(
                "org.apache.wss4j.dom.action.Action",
                "org.apache.wss4j.dom.processor.Processor",
                "org.apache.wss4j.dom.validate.Validator")
                .map(DotName::createSimple)
                .flatMap(dotName -> index.getAllKnownImplementors(dotName).stream())
                .map(classInfo -> classInfo.name().toString())
                .map(className -> new ReflectiveClassBuildItem(false, false, className))
                .forEach(reflectiveClass::produce);

    }

    @BuildStep
    void runtimeInitializedClass(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClass) {
        Stream.of(
                "org.apache.wss4j.common.saml.builder.SAML1ComponentBuilder",
                "org.apache.wss4j.common.saml.builder.SAML2ComponentBuilder")
                .map(RuntimeInitializedClassBuildItem::new)
                .forEach(runtimeInitializedClass::produce);
    }

    @BuildStep
    void resourceBundle(BuildProducer<NativeImageResourceBundleBuildItem> resourceBundle) {
        resourceBundle.produce(
                new NativeImageResourceBundleBuildItem("messages.wss4j_errors"));
    }

}
