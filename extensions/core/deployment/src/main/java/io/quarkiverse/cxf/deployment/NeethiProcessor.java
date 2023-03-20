package io.quarkiverse.cxf.deployment;

import java.util.Set;
import java.util.stream.Stream;

import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

/**
 * {@link BuildStep}s related to {@code org.apache.neethi:neethi}
 */
class NeethiProcessor {

    /** These depend on Apache Axiom that CXF 4 is excluding */
    private static final Set<String> BANNED_CONVERTERS = Set.of(
            "org.apache.neethi.builders.converters.StaxToOMConverter",
            "org.apache.neethi.builders.converters.OMToStaxConverter",
            "org.apache.neethi.builders.converters.OMToDOMConverter",
            "org.apache.neethi.builders.converters.DOMToOMConverter",
            "org.apache.neethi.builders.converters.OMToOMConverter");

    @BuildStep
    void indexDependencies(BuildProducer<IndexDependencyBuildItem> indexDependencies) {
        Stream.of(
                "org.apache.neethi:neethi")
                .forEach(ga -> {
                    String[] coords = ga.split(":");
                    indexDependencies.produce(new IndexDependencyBuildItem(coords[0], coords[1]));
                });
    }

    @BuildStep
    void reflectiveClass(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        final IndexView index = combinedIndexBuildItem.getIndex();

        index.getAllKnownImplementors(DotName.createSimple("org.apache.neethi.builders.converters.Converter")).stream()
                .map(classInfo -> classInfo.name().toString())
                .filter(cl -> !BANNED_CONVERTERS.contains(cl))
                .map(className -> ReflectiveClassBuildItem.builder(className).methods().build())
                .forEach(reflectiveClass::produce);

        final String abstractDomCoverter = "org.apache.neethi.builders.converters.AbstractDOMConverter";
        /* Register AbstractDOMConverter itself and its subclasses */
        Stream.concat(Stream.of(abstractDomCoverter),
                index.getAllKnownSubclasses(DotName.createSimple(abstractDomCoverter)).stream()
                        .map(classInfo -> classInfo.name().toString()))
                .filter(cl -> !BANNED_CONVERTERS.contains(cl))
                .map(className -> ReflectiveClassBuildItem.builder(className).methods().build())
                .forEach(reflectiveClass::produce);

    }

}
