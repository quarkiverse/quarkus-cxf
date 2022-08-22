package io.quarkiverse.cxf.deployment;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;

public class QuarkusCxfWsSecurityProcessor {

    private static final Logger LOGGER = Logger.getLogger(QuarkusCxfWsSecurityProcessor.class);

    private static final List<String> interfaceImplsToRegister = Arrays.asList(
            "javax.xml.soap.SOAPBodyElement",
            "com.sun.xml.bind.v2.runtime.property.Property",
            "org.ehcache.core.spi.service.ServiceFactory");

    @BuildStep
    void indexDependencies(BuildProducer<IndexDependencyBuildItem> indexDependencies) {
        Stream.of(
                "org.jboss.spec.javax.xml.bind:jboss-jaxb-api_2.3_spec",
                "org.ehcache:ehcache",
                "jakarta.xml.soap:jakarta.xml.soap-api",
                "org.apache.wss4j:wss4j-ws-security-dom")
                .forEach(ga -> {
                    String[] coords = ga.split(":");
                    indexDependencies.produce(new IndexDependencyBuildItem(coords[0], coords[1]));
                });
    }

    @BuildStep
    void registerWsSecurityReflectionItems(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveItems) {
        IndexView indexView = combinedIndexBuildItem.getIndex();

        indexView.getKnownClasses().stream()
                .map(ci -> ci.name().toString())
                .filter(c -> c.startsWith("org.ehcache.xml.model."))
                .forEach(c -> reflectiveItems.produce(new ReflectiveClassBuildItem(true, true, c)));

        indexView.getKnownClasses().stream()
                .map(ci -> ci.name().toString())
                .filter(c -> (c.startsWith("javax.xml.bind.annotation.") ||
                        c.startsWith("org.apache.wss4j.dom.transform.") ||
                        c.startsWith("org.apache.wss4j.dom.action.") ||
                        c.startsWith("org.apache.wss4j.dom.processor.") ||
                        c.startsWith("org.apache.wss4j.dom.validate.")) && !c.contains("$"))
                .forEach(c -> reflectiveItems.produce(new ReflectiveClassBuildItem(true, false, c)));

        interfaceImplsToRegister.stream()
                .forEach(intf -> indexView.getAllKnownImplementors(DotName.createSimple(intf)).stream()
                        .forEach(implementor -> reflectiveItems
                                .produce(new ReflectiveClassBuildItem(true, false, implementor.name().toString()))));

        reflectiveItems.produce(new ReflectiveClassBuildItem(true, false,
                "javax.xml.ws.EndpointReference",

                "org.apache.cxf.ws.security.policy.WSSecurityPolicyLoader",
                "org.apache.cxf.ws.security.tokenstore.SecurityToken",
                "org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor",
                "org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor",

                "org.apache.cxf.catalog.OASISCatalogManager",
                "org.apache.cxf.common.util.ASMHelperImpl$2",
                "org.apache.cxf.common.util.OpcodesProxy",
                "org.apache.cxf.headers.HeaderManager",
                "org.apache.cxf.policy.PolicyDataEngine",

                "org.apache.xml.resolver.CatalogManager",
                "org.apache.xml.security.c14n.implementations.CanonicalizerPhysical",
                "org.apache.xml.security.utils.XMLUtils",

                "org.ehcache.jsr107.internal.Jsr107ServiceConfigurationParser",
                "org.ehcache.jsr107.internal.Jsr107CacheConfigurationParser"));

        reflectiveItems.produce(new ReflectiveClassBuildItem(true, true,
                "org.apache.cxf.ws.security.cache.CacheCleanupListener",

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

        reflectiveItems.produce(ReflectiveClassBuildItem.serializationClass(
                "java.lang.Enum",
                "java.lang.String",
                "java.time.Instant",
                "java.time.Ser",
                "java.util.ArrayList",
                "java.util.Arrays$ArrayList",
                "java.util.Collections$EmptyMap",
                "java.util.Collections$SingletonList",
                "java.util.HashMap",
                "org.apache.wss4j.common.cache.EHCacheValue"));
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
    NativeImageResourceBuildItem wsSecurityResources() {
        return new NativeImageResourceBuildItem(
                "META-INF/services/org.ehcache.core.spi.service.ServiceFactory",
                "META-INF/services/org.ehcache.xml.CacheManagerServiceConfigurationParser",
                "META-INF/services/org.ehcache.xml.CacheServiceConfigurationParser",
                "cxf-ehcache.xml",
                "ehcache-107-ext.xsd",
                "ehcache-core.xsd");
    }

    @BuildStep
    void xmlSecurityResourceBundle(BuildProducer<NativeImageResourceBundleBuildItem> resourceBundle) {
        resourceBundle.produce(
                new NativeImageResourceBundleBuildItem("org.apache.xml.security.resource.xmlsecurity"));
        resourceBundle.produce(
                new NativeImageResourceBundleBuildItem("messages.wss4j_errors"));
    }

    @BuildStep
    List<RuntimeInitializedClassBuildItem> runtimeInitializedClasses(CombinedIndexBuildItem combinedIndexBuildItem) {

        return Arrays.asList(
                new RuntimeInitializedClassBuildItem("org.apache.wss4j.common.saml.builder.SAML1ComponentBuilder"),
                new RuntimeInitializedClassBuildItem("org.apache.wss4j.common.saml.builder.SAML2ComponentBuilder"),
                new RuntimeInitializedClassBuildItem("org.apache.xml.security.stax.impl.InboundSecurityContextImpl"),
                new RuntimeInitializedClassBuildItem("org.ehcache.shadow.org.terracotta.utilities.io.Files"),
                new RuntimeInitializedClassBuildItem(
                        "org.ehcache.shadow.org.terracotta.offheapstore.storage.portability.BooleanPortability"));
    }

    @BuildStep
    void runtimeReinitializedClass(BuildProducer<RuntimeInitializedClassBuildItem> runtimeReinitializedClass) {
        Stream.of("org.ehcache.xml.XmlConfiguration",
                "org.ehcache.xml.ResourceConfigurationParser")
                .map(RuntimeInitializedClassBuildItem::new)
                .forEach(runtimeReinitializedClass::produce);
    }

}
