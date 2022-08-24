package io.quarkiverse.cxf.deployment;

import java.util.stream.Stream;

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

        index.getKnownClasses().stream()
                .map(ci -> ci.name().toString())
                .filter(c -> (c.startsWith("org.apache.wss4j.dom.transform.") ||
                        c.startsWith("org.apache.wss4j.dom.action.") ||
                        c.startsWith("org.apache.wss4j.dom.processor.") ||
                        c.startsWith("org.apache.wss4j.dom.validate.")) && !c.contains("$"))
                .map(className -> new ReflectiveClassBuildItem(true, false, className))
                .forEach(reflectiveClass::produce);

        reflectiveClass.produce(ReflectiveClassBuildItem.serializationClass(
                "org.apache.wss4j.common.cache.EHCacheValue"));

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
