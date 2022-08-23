package io.quarkiverse.cxf.deployment;

import java.util.stream.Stream;

import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

/**
 * {@link BuildStep}s related to {@code org.glassfish.jaxb:*}.
 */
class JaxbProcessor {

    @BuildStep
    void indexDependencies(BuildProducer<IndexDependencyBuildItem> indexDependencies) {
        Stream.of(
                "org.glassfish.jaxb:txw2",
                "org.glassfish.jaxb:jaxb-runtime")
                .forEach(ga -> {
                    String[] coords = ga.split(":");
                    indexDependencies.produce(new IndexDependencyBuildItem(coords[0], coords[1]));
                });
    }

    @BuildStep
    void registerWsSecurityReflectionItems(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        final IndexView index = combinedIndexBuildItem.getIndex();

        index.getAllKnownSubclasses(DotName.createSimple("com.sun.xml.bind.v2.runtime.JaxBeanInfo")).stream()
                .map(classInfo -> classInfo.name().toString())
                .map(className -> new ReflectiveClassBuildItem(true, false, className))
                .forEach(reflectiveClass::produce);

        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false,
                "com.sun.xml.bind.v2.runtime.JAXBContextImpl",
                "com.sun.xml.bind.v2.runtime.JaxBeanInfo"));

    }

}
