package io.quarkiverse.cxf.deployment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Stream;

import org.jboss.jandex.IndexView;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
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
                "org.ehcache:ehcache")
                .forEach(ga -> {
                    String[] coords = ga.split(":");
                    indexDependencies.produce(new IndexDependencyBuildItem(coords[0], coords[1]));
                });
    }

    @BuildStep
    void registerServices(BuildProducer<ServiceProviderBuildItem> serviceProvider) {
        Stream.of(
                "org.ehcache.core.spi.service.ServiceFactory",
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

        index.getKnownClasses().stream()
                .map(ci -> ci.name().toString())
                .filter(c -> c.startsWith("org.ehcache.xml.model."))
                .map(className -> new ReflectiveClassBuildItem(true, true, className))
                .forEach(reflectiveClass::produce);

        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true,
                "org.ehcache.CacheManager",
                "org.ehcache.core.Ehcache",
                "org.ehcache.core.EhcacheBase",
                "org.ehcache.core.internal.statistics.StatsUtils$1",
                "org.ehcache.core.internal.statistics.StatsUtils$2",
                "org.ehcache.core.internal.statistics.StatsUtils$3",
                "org.ehcache.core.spi.service.StatisticsService",
                "org.ehcache.impl.copy.IdentityCopier",
                "org.ehcache.impl.internal.concurrent.ConcurrentHashMap",
                "org.ehcache.impl.internal.concurrent.ConcurrentHashMap$CounterCell",
                "org.ehcache.impl.internal.concurrent.ConcurrentHashMap$TreeBin",
                "org.ehcache.impl.internal.resilience.RobustResilienceStrategy",
                "org.ehcache.impl.internal.store.disk.OffHeapDiskStore",
                "org.ehcache.impl.internal.store.heap.OnHeapStore",
                "org.ehcache.impl.internal.store.offheap.AbstractOffHeapStore",
                "org.ehcache.impl.internal.store.tiering.TieredStore",
                "org.ehcache.impl.serialization.CompactJavaSerializer",
                "org.ehcache.impl.serialization.StringSerializer",
                "org.ehcache.impl.store.BaseStore",
                "org.ehcache.shadow.org.terracotta.context.query.Matchers$1",
                "org.ehcache.shadow.org.terracotta.context.query.Matchers$2",
                "org.ehcache.shadow.org.terracotta.context.query.Matchers$3",
                "org.ehcache.shadow.org.terracotta.context.query.Matchers$4",
                "org.ehcache.shadow.org.terracotta.context.query.Matchers$5",
                "org.ehcache.shadow.org.terracotta.context.query.Matchers$6",
                "org.ehcache.shadow.org.terracotta.context.query.Matchers$8",
                "org.ehcache.shadow.org.terracotta.offheapstore.storage.portability.Portability",
                "org.ehcache.shadow.org.terracotta.statistics.AbstractOperationStatistic",
                "org.ehcache.shadow.org.terracotta.statistics.AbstractSourceStatistic",
                "org.ehcache.shadow.org.terracotta.statistics.GeneralOperationStatistic",
                "org.ehcache.shadow.org.terracotta.statistics.MappedOperationStatistic",
                "org.ehcache.shadow.org.terracotta.statistics.MappedOperationStatistic$1",
                "org.ehcache.shadow.org.terracotta.statistics.PassThroughStatistic"));

        reflectiveClass.produce(ReflectiveClassBuildItem.serializationClass(
                "java.lang.Enum",
                "java.lang.String",
                "java.time.Instant",
                "java.time.Ser",
                "java.util.ArrayList",
                "java.util.Arrays$ArrayList",
                "java.util.Collections$EmptyMap",
                "java.util.Collections$SingletonList",
                "java.util.HashMap"));

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
    void runtimeInitializedClass(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClass) {
        Stream.of(
                "org.ehcache.shadow.org.terracotta.utilities.io.Files",
                "org.ehcache.shadow.org.terracotta.offheapstore.storage.portability.BooleanPortability",
                "org.ehcache.xml.XmlConfiguration",
                "org.ehcache.xml.ResourceConfigurationParser")
                .map(RuntimeInitializedClassBuildItem::new)
                .forEach(runtimeInitializedClass::produce);
    }

}
