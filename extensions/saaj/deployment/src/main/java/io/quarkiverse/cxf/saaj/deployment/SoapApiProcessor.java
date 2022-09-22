package io.quarkiverse.cxf.saaj.deployment;

import java.util.stream.Stream;

import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

/**
 * {@link BuildStep}s related to {@code jakarta.xml.soap:jakarta.xml.soap-api}
 */
class SoapApiProcessor {

    @BuildStep
    void indexDependencies(BuildProducer<IndexDependencyBuildItem> indexDependencies) {
        Stream.of(
                "jakarta.xml.soap:jakarta.xml.soap-api")
                .forEach(ga -> {
                    String[] coords = ga.split(":");
                    indexDependencies.produce(new IndexDependencyBuildItem(coords[0], coords[1]));
                });
    }

    @BuildStep
    void reflectiveClass(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        final IndexView index = combinedIndexBuildItem.getIndex();

        index.getAllKnownImplementors(DotName.createSimple("javax.xml.soap.SOAPBodyElement")).stream()
                .map(classInfo -> classInfo.name().toString())
                .map(className -> new ReflectiveClassBuildItem(true, false, className))
                .forEach(reflectiveClass::produce);

    }

}
