package io.quarkiverse.cxf.deployment;

import java.util.stream.Stream;

import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

/**
 * {@link BuildStep}s related to {@code jakarta.activation}
 */
class JakartaActivationProcessor {

    @BuildStep
    void indexDependencies(BuildProducer<IndexDependencyBuildItem> indexDependencies) {
        Stream.of(
                "com.sun.activation:jakarta.activation")
                .forEach(ga -> {
                    String[] coords = ga.split(":");
                    indexDependencies.produce(new IndexDependencyBuildItem(coords[0], coords[1]));
                });
    }

    @BuildStep
    public void nativeResources(BuildProducer<NativeImageResourceBuildItem> nativeResources) {
        Stream.of("META-INF/mailcap.default",
                "META-INF/mimetypes.default")
                .map(NativeImageResourceBuildItem::new)
                .forEach(nativeResources::produce);
    }

    @BuildStep
    void reflectiveClasses(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        final IndexView index = combinedIndexBuildItem.getIndex();

        index.getAllKnownImplementors(DotName.createSimple("javax.activation.DataContentHandler")).stream()
                .map(classInfo -> classInfo.name().toString())
                .map(className -> new ReflectiveClassBuildItem(false, false, className))
                .forEach(reflectiveClasses::produce);

        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, false, "java.beans.Beans"));
    }
}
