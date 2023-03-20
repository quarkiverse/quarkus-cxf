package io.quarkiverse.cxf.ws.security.deployment;

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
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageSystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.util.ServiceUtil;

/**
 * {@link BuildStep}s related to {@code org.ehcache:*}
 */
public class EhcacheProcessor {

    @BuildStep
    void indexDependencies(BuildProducer<IndexDependencyBuildItem> indexDependencies) {
        Stream.of(
                "org.ehcache:ehcache:jakarta")
                .forEach(ga -> {
                    String[] coords = ga.split(":");
                    indexDependencies.produce(new IndexDependencyBuildItem(coords[0], coords[1], coords[2]));
                });
    }

    @BuildStep
    void registerServices(BuildProducer<ServiceProviderBuildItem> serviceProvider) {
        Stream.of(
                "org.ehcache.core.spi.service.ServiceFactory",
                "org.ehcache.core.spi.service.StatisticsService",
                "org.ehcache.xml.CacheManagerServiceConfigurationParser",
                "org.ehcache.xml.CacheServiceConfigurationParser")
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

    @BuildStep
    void reflectiveClass(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        final IndexView index = combinedIndexBuildItem.getIndex();

        Stream.of(
                "org.ehcache.spi.copy.Copier",
                "org.ehcache.spi.resilience.ResilienceStrategy",
                "org.ehcache.spi.serialization.Serializer")
                .map(DotName::createSimple)
                .flatMap(dotName -> index.getAllKnownImplementors(dotName).stream())
                .map(classInfo -> classInfo.name().toString())
                .map(className -> ReflectiveClassBuildItem.builder(className).build())
                .forEach(reflectiveClass::produce);

        Stream.of(
                "org.ehcache.shadow.org.terracotta.statistics.SourceStatistic")
                .map(DotName::createSimple)
                .flatMap(dotName -> index.getAllKnownImplementors(dotName).stream())
                .map(classInfo -> classInfo.name().toString())
                .map(className -> ReflectiveClassBuildItem.builder(className).fields().build())
                .forEach(reflectiveClass::produce);

        Stream.of(
                "org.ehcache.shadow.org.terracotta.context.query.Matcher")
                .map(DotName::createSimple)
                .flatMap(dotName -> index.getAllKnownSubclasses(dotName).stream())
                .map(classInfo -> classInfo.name().toString())
                .map(className -> ReflectiveClassBuildItem.builder(className).methods().build())
                .forEach(reflectiveClass::produce);

    }

    @BuildStep
    void proxyItems(BuildProducer<NativeImageProxyDefinitionBuildItem> proxies) {
        proxies.produce(new NativeImageProxyDefinitionBuildItem(
                "org.ehcache.shadow.org.terracotta.offheapstore.storage.portability.Portability",
                "org.ehcache.shadow.org.terracotta.offheapstore.disk.persistent.PersistentPortability"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem(
                "org.ehcache.shadow.org.terracotta.offheapstore.storage.portability.WriteBackPortability",
                "org.ehcache.shadow.org.terracotta.offheapstore.disk.persistent.PersistentPortability"));
    }

    @BuildStep
    NativeImageResourceBuildItem nativeImageResource() {
        return new NativeImageResourceBuildItem(
                "cxf-ehcache.xml",
                "ehcache-107-ext.xsd",
                "ehcache-core.xsd");
    }

    @BuildStep
    void nativeImageSystemProperties(BuildProducer<NativeImageSystemPropertyBuildItem> nativeImageSystemProperties) {
        nativeImageSystemProperties
                .produce(new NativeImageSystemPropertyBuildItem("org.ehcache.sizeof.AgentSizeOf.bypass", "true"));
    }

    @BuildStep
    void runtimeInitializedClass(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClass) {
        Stream.of(
                "org.ehcache.sizeof.impl.JvmInformation",
                "org.ehcache.shadow.org.terracotta.utilities.io.Files",
                "org.ehcache.shadow.org.terracotta.offheapstore.storage.portability.BooleanPortability",
                "org.ehcache.xml.XmlConfiguration",
                "org.ehcache.xml.ResourceConfigurationParser")
                .map(RuntimeInitializedClassBuildItem::new)
                .forEach(runtimeInitializedClass::produce);
    }

}
