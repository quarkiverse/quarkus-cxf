package io.quarkiverse.cxf.deployment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Stream;

import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.util.ServiceUtil;

/**
 * {@link BuildStep}s related to {@code jakarta.xml.ws:jakarta.xml.ws-api}
 */
class WsApiProcessor {

    @BuildStep
    void indexDependencies(BuildProducer<IndexDependencyBuildItem> indexDependencies) {
        Stream.of(
                "jakarta.xml.ws:jakarta.xml.ws-api")
                .forEach(ga -> {
                    String[] coords = ga.split(":");
                    indexDependencies.produce(new IndexDependencyBuildItem(coords[0], coords[1]));
                });
    }

    @BuildStep
    void registerForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass, CombinedIndexBuildItem combinedIndex) {

        final IndexView index = combinedIndex.getIndex();
        Stream.of(
                "javax.xml.ws.handler.Handler")
                .map(DotName::createSimple)
                .flatMap(dotName -> index.getAllKnownImplementors(dotName).stream())
                .map(classInfo -> classInfo.name().toString())
                .map(className -> new ReflectiveClassBuildItem(false, false, className))
                .forEach(reflectiveClass::produce);
    }

    @BuildStep
    void registerServices(BuildProducer<ServiceProviderBuildItem> serviceProvider) {
        Stream.of(
                "javax.xml.ws.spi.Provider")
                .forEach(serviceName -> {
                    try {
                        final Set<String> names = ServiceUtil.classNamesNamedIn(Thread.currentThread().getContextClassLoader(),
                                ServiceProviderBuildItem.SPI_ROOT + serviceName);
                        serviceProvider.produce(new ServiceProviderBuildItem(serviceName, new ArrayList<>(names)));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

}
