package io.quarkiverse.cxf.deployment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import io.quarkiverse.cxf.graal.IsWsSecurityPresent;
import org.jboss.logging.Logger;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;

public class QuarkusCxfWsSecurityProcessor {

    private static final Logger LOGGER = Logger.getLogger(QuarkusCxfWsSecurityProcessor.class);

    @BuildStep(onlyIf = IsWsSecurityPresent.class)
    void registerWsSecurityReflectionItems(BuildProducer<ReflectiveClassBuildItem> reflectiveItems) {
        reflectiveItems.produce(new ReflectiveClassBuildItem(true, false,
                "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl",
                "com.sun.org.apache.xerces.internal.impl.dv.xs.ExtendedSchemaDVFactoryImpl",
                "com.sun.org.apache.xerces.internal.impl.dv.xs.SchemaDVFactoryImpl",
                "com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl",
                "com.sun.xml.bind.v2.runtime.property.ArrayElementLeafProperty",
                "com.sun.xml.bind.v2.runtime.property.ArrayElementNodeProperty",
                "com.sun.xml.bind.v2.runtime.property.ArrayReferenceNodeProperty",
                "com.sun.xml.bind.v2.runtime.property.SingleElementLeafProperty",
                "com.sun.xml.bind.v2.runtime.property.SingleElementNodeProperty",
                "com.sun.xml.bind.v2.runtime.property.SingleMapNodeProperty",
                "com.sun.xml.bind.v2.runtime.property.SingleReferenceNodeProperty",
                "com.sun.xml.messaging.saaj.soap.impl.ElementImpl",
                "com.sun.xml.messaging.saaj.soap.impl.SOAPTextImpl",
                "com.sun.xml.messaging.saaj.soap.impl.TextImpl",
                "com.sun.xml.messaging.saaj.soap.ver1_1.Body1_1Impl",
                "com.sun.xml.messaging.saaj.soap.ver1_1.BodyElement1_1Impl",
                "com.sun.xml.messaging.saaj.soap.ver1_1.Header1_1Impl",
                "com.sun.xml.messaging.saaj.soap.ver1_1.HeaderElement1_1Impl",
                "com.sun.xml.messaging.saaj.soap.ver1_2.Body1_2Impl",
                "com.sun.xml.messaging.saaj.soap.ver1_2.BodyElement1_2Impl",
                "com.sun.xml.messaging.saaj.soap.ver1_2.Header1_2Impl",
                "com.sun.xml.messaging.saaj.soap.ver1_2.HeaderElement1_2Impl",

                "javax.xml.bind.annotation.W3CDomHandler",
                "javax.xml.bind.annotation.XmlAccessorType",
                "javax.xml.bind.annotation.XmlAnyElement",
                "javax.xml.bind.annotation.XmlElement",
                "javax.xml.bind.annotation.XmlElementDecl",
                "javax.xml.bind.annotation.XmlElementRef",
                "javax.xml.bind.annotation.XmlElements",
                "javax.xml.bind.annotation.XmlEnum",
                "javax.xml.bind.annotation.XmlEnumValue",
                "javax.xml.bind.annotation.XmlID",
                "javax.xml.bind.annotation.XmlIDREF",
                "javax.xml.bind.annotation.XmlRegistry",
                "javax.xml.bind.annotation.XmlType",
                "javax.xml.bind.annotation.adapters.CollapsedStringAdapter",
                "javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter",
                "javax.xml.ws.EndpointReference",

                "org.apache.cxf.catalog.OASISCatalogManager",
                "org.apache.cxf.common.util.ASMHelperImpl$2",
                "org.apache.cxf.common.util.OpcodesProxy",
                "org.apache.cxf.headers.HeaderManager",
                "org.apache.cxf.policy.PolicyDataEngine",
                "org.apache.cxf.ws.security.policy.WSSecurityPolicyLoader",
                "org.apache.cxf.ws.security.tokenstore.SecurityToken",
                "org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor",
                "org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor",

                "org.apache.wss4j.dom.action.CustomTokenAction",
                "org.apache.wss4j.dom.action.EncryptionAction",
                "org.apache.wss4j.dom.action.EncryptionDerivedAction",
                "org.apache.wss4j.dom.action.SAMLTokenSignedAction",
                "org.apache.wss4j.dom.action.SAMLTokenUnsignedAction",
                "org.apache.wss4j.dom.action.SignatureAction",
                "org.apache.wss4j.dom.action.SignatureConfirmationAction",
                "org.apache.wss4j.dom.action.SignatureDerivedAction",
                "org.apache.wss4j.dom.action.TimestampAction",
                "org.apache.wss4j.dom.action.UsernameTokenAction",
                "org.apache.wss4j.dom.action.UsernameTokenSignedAction",

                "org.apache.wss4j.dom.processor.BinarySecurityTokenProcessor",
                "org.apache.wss4j.dom.processor.DerivedKeyTokenProcessor",
                "org.apache.wss4j.dom.processor.EncryptedAssertionProcessor",
                "org.apache.wss4j.dom.processor.EncryptedDataProcessor",
                "org.apache.wss4j.dom.processor.EncryptedKeyProcessor",
                "org.apache.wss4j.dom.processor.ReferenceListProcessor",
                "org.apache.wss4j.dom.processor.SAMLTokenProcessor",
                "org.apache.wss4j.dom.processor.SecurityContextTokenProcessor",
                "org.apache.wss4j.dom.processor.SignatureConfirmationProcessor",
                "org.apache.wss4j.dom.processor.SignatureProcessor",
                "org.apache.wss4j.dom.processor.TimestampProcessor",
                "org.apache.wss4j.dom.processor.UsernameTokenProcessor",

                "org.apache.wss4j.dom.transform.AttachmentCiphertextTransform",
                "org.apache.wss4j.dom.transform.AttachmentCompleteSignatureTransform",
                "org.apache.wss4j.dom.transform.AttachmentCompleteSignatureTransformProvider",
                "org.apache.wss4j.dom.transform.AttachmentContentSignatureTransform",
                "org.apache.wss4j.dom.transform.AttachmentContentSignatureTransformProvider",
                "org.apache.wss4j.dom.transform.AttachmentTransformParameterSpec",
                "org.apache.wss4j.dom.transform.STRTransform",
                "org.apache.wss4j.dom.transform.STRTransformProvider",
                "org.apache.wss4j.dom.transform.STRTransformUtil",

                "org.apache.wss4j.dom.validate.Credential",
                "org.apache.wss4j.dom.validate.JAASUsernameTokenValidator",
                "org.apache.wss4j.dom.validate.KerberosTokenValidator",
                "org.apache.wss4j.dom.validate.NoOpValidator",
                "org.apache.wss4j.dom.validate.SamlAssertionValidator",
                "org.apache.wss4j.dom.validate.SignatureTrustValidator",
                "org.apache.wss4j.dom.validate.TimestampValidator",
                "org.apache.wss4j.dom.validate.UsernameTokenValidator",

                "org.apache.xml.resolver.CatalogManager",
                "org.apache.xml.security.c14n.implementations.CanonicalizerPhysical",
                "org.apache.xml.security.utils.XMLUtils",

                "com.sun.xml.messaging.saaj.soap.SAAJMetaFactoryImpl",
                "org.ehcache.jsr107.internal.Jsr107ServiceConfigurationParser",
                "org.ehcache.jsr107.internal.Jsr107CacheConfigurationParser",
                "org.ehcache.impl.internal.TimeSourceServiceFactory",
                "org.ehcache.impl.internal.events.CacheEventNotificationListenerServiceProviderFactory",
                "org.ehcache.impl.internal.executor.DefaultExecutionServiceFactory",
                "org.ehcache.impl.internal.loaderwriter.writebehind.WriteBehindProviderFactory",
                "org.ehcache.impl.internal.persistence.DefaultDiskResourceServiceFactory",
                "org.ehcache.impl.internal.persistence.DefaultLocalPersistenceServiceFactory",
                "org.ehcache.impl.internal.resilience.RobustLoaderWriterResilienceStrategy",
                "org.ehcache.impl.internal.resilience.RobustResilienceStrategy",
                "org.ehcache.impl.internal.sizeof.DefaultSizeOfEngineProviderFactory",
                "org.ehcache.impl.internal.spi.copy.DefaultCopyProviderFactory",
                "org.ehcache.impl.internal.spi.event.DefaultCacheEventListenerProviderFactory",
                "org.ehcache.impl.internal.spi.loaderwriter.DefaultCacheLoaderWriterProviderFactory",
                "org.ehcache.impl.internal.spi.resilience.DefaultResilienceStrategyProviderFactory",
                "org.ehcache.impl.internal.spi.serialization.DefaultSerializationProviderFactory",
                "org.ehcache.impl.internal.store.disk.OffHeapDiskStoreProviderFactory",
                "org.ehcache.impl.internal.store.heap.OnHeapStoreProviderFactory",
                "org.ehcache.impl.internal.store.loaderwriter.LoaderWriterStoreProviderFactory",
                "org.ehcache.impl.internal.store.offheap.OffHeapStoreProviderFactory",
                "org.ehcache.impl.internal.store.tiering.CompoundCachingTierProviderFactory",
                "org.ehcache.impl.internal.store.tiering.TieredStoreProviderFactory",
                "org.ehcache.core.internal.statistics.DefaultStatisticsServiceFactory"));

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
                "org.ehcache.shadow.org.terracotta.statistics.PassThroughStatistic",

                "org.ehcache.xml.model.Adapter2",
                "org.ehcache.xml.model.BaseCacheType",
                "org.ehcache.xml.model.CacheEntryType",
                "org.ehcache.xml.model.CacheLoaderWriterType",
                "org.ehcache.xml.model.CacheLoaderWriterType$WriteBehind",
                "org.ehcache.xml.model.CacheLoaderWriterType$WriteBehind$Batching",
                "org.ehcache.xml.model.CacheLoaderWriterType$WriteBehind$NonBatching",
                "org.ehcache.xml.model.CacheTemplateType",
                "org.ehcache.xml.model.CacheType",
                "org.ehcache.xml.model.ConfigType",
                "org.ehcache.xml.model.CopierType",
                "org.ehcache.xml.model.CopierType$Copier",
                "org.ehcache.xml.model.Disk",
                "org.ehcache.xml.model.DiskStoreSettingsType",
                "org.ehcache.xml.model.EventFiringType",
                "org.ehcache.xml.model.EventOrderingType",
                "org.ehcache.xml.model.EventType",
                "org.ehcache.xml.model.ExpiryType",
                "org.ehcache.xml.model.ExpiryType$None",
                "org.ehcache.xml.model.Heap",
                "org.ehcache.xml.model.ListenersType",
                "org.ehcache.xml.model.ListenersType$Listener",
                "org.ehcache.xml.model.MemoryType",
                "org.ehcache.xml.model.MemoryTypeWithPropSubst",
                "org.ehcache.xml.model.MemoryUnit",
                "org.ehcache.xml.model.ObjectFactory",
                "org.ehcache.xml.model.Offheap",
                "org.ehcache.xml.model.PersistableMemoryTypeWithPropSubst",
                "org.ehcache.xml.model.PersistenceType",
                "org.ehcache.xml.model.ResourceTypeWithPropSubst",
                "org.ehcache.xml.model.ResourceUnit",
                "org.ehcache.xml.model.ResourcesType",
                "org.ehcache.xml.model.SerializerType",
                "org.ehcache.xml.model.SerializerType$Serializer",
                "org.ehcache.xml.model.ServiceType",
                "org.ehcache.xml.model.SizeofType",
                "org.ehcache.xml.model.SizeofType$MaxObjectGraphSize",
                "org.ehcache.xml.model.ThreadPoolReferenceType",
                "org.ehcache.xml.model.ThreadPoolsType",
                "org.ehcache.xml.model.ThreadPoolsType$ThreadPool",
                "org.ehcache.xml.model.TimeTypeWithPropSubst",
                "org.ehcache.xml.model.TimeUnit"));

        reflectiveItems.produce(
                ReflectiveClassBuildItem.serializationClass(
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

    @BuildStep(onlyIf = IsWsSecurityPresent.class)
    void proxyItems(BuildProducer<NativeImageProxyDefinitionBuildItem> proxies) {
        proxies.produce(new NativeImageProxyDefinitionBuildItem(
                "org.ehcache.shadow.org.terracotta.offheapstore.storage.portability.Portability",
                "org.ehcache.shadow.org.terracotta.offheapstore.disk.persistent.PersistentPortability"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem(
                "org.ehcache.shadow.org.terracotta.offheapstore.storage.portability.WriteBackPortability",
                "org.ehcache.shadow.org.terracotta.offheapstore.disk.persistent.PersistentPortability"));
    }

    @BuildStep(onlyIf = IsWsSecurityPresent.class)
    NativeImageResourceBuildItem wsSecurityResources() {
        return new NativeImageResourceBuildItem(
                "META-INF/services/javax.xml.soap.SAAJMetaFactory",
                "META-INF/services/javax.xml.stream.XMLEventFactory",
                "META-INF/services/javax.xml.stream.XMLInputFactory",
                "META-INF/services/javax.xml.stream.XMLOutputFactory",
                "META-INF/services/org.ehcache.core.spi.service.ServiceFactory",
                "META-INF/services/org.ehcache.xml.CacheManagerServiceConfigurationParser",
                "META-INF/services/org.ehcache.xml.CacheServiceConfigurationParser",
                "cxf-ehcache.xml",
                "ehcache-107-ext.xsd",
                "ehcache-core.xsd",
                "messages/wss4j_errors.properties");
    }

    @BuildStep(onlyIf = IsWsSecurityPresent.class)
    void buildMergedResources(BuildProducer<NativeImageResourceBuildItem> nativeResources,
            BuildProducer<GeneratedResourceBuildItem> generatedResources) {

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(os));
        try {
            URL wss4jErrors = Thread.currentThread().getContextClassLoader()
                    .getResource("messages/wss4j_errors.properties");
            URL xmlsecurity = Thread.currentThread().getContextClassLoader()
                    .getResource("org/apache/xml/security/resource/xmlsecurity_en.properties");
            try (InputStream openStream = wss4jErrors.openStream()) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(openStream));
                String line = reader.readLine();
                while (line != null) {
                    out.write(line);
                    out.newLine();
                    line = reader.readLine();
                }
            }
            try (InputStream openStream = xmlsecurity.openStream()) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(openStream));
                String line = reader.readLine();
                while (line != null) {
                    out.write(line);
                    out.newLine();
                    line = reader.readLine();
                }
            }
        } catch (IOException e) {
            LOGGER.warn("cannot merge wss4j_errors and xmlsecurity properties");
        }
        if (os.size() > 0) {
            generatedResources.produce(
                    new GeneratedResourceBuildItem("org/apache/xml/security/resource/xmlsecurity.properties",
                            os.toByteArray()));
            nativeResources
                    .produce(new NativeImageResourceBuildItem("org/apache/xml/security/resource/xmlsecurity.properties"));
        }
    }

    @BuildStep(onlyIf = IsWsSecurityPresent.class)
    List<RuntimeInitializedClassBuildItem> runtimeInitializedClasses() {
        return Arrays.asList(
                new RuntimeInitializedClassBuildItem("com.sun.xml.bind.v2.runtime.output.XMLStreamWriterOutput"),
                new RuntimeInitializedClassBuildItem("org.apache.wss4j.common.saml.builder.SAML1ComponentBuilder"),
                new RuntimeInitializedClassBuildItem("org.apache.wss4j.common.saml.builder.SAML2ComponentBuilder"),
                new RuntimeInitializedClassBuildItem("org.apache.wss4j.stax.setup.WSSec"),
                new RuntimeInitializedClassBuildItem("org.apache.xml.security.stax.ext.XMLSecurityConstants"),
                new RuntimeInitializedClassBuildItem("org.apache.xml.security.stax.impl.InboundSecurityContextImpl"),
                new RuntimeInitializedClassBuildItem(
                        "org.apache.xml.security.stax.impl.processor.input.XMLEventReaderInputProcessor"),
                new RuntimeInitializedClassBuildItem("org.terracotta.utilities.io.Files"),
                new RuntimeInitializedClassBuildItem(
                        "org.ehcache.shadow.org.terracotta.offheapstore.storage.portability.BooleanPortability"));
    }

}
