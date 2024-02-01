package io.quarkiverse.xmlsec.deployment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Stream;

import javax.crypto.spec.GCMParameterSpec;
import javax.xml.crypto.dsig.spec.XPathType;

import org.apache.jcp.xml.dsig.internal.dom.XMLDSigRI;
import org.apache.xml.security.algorithms.SignatureAlgorithmSpi;
import org.apache.xml.security.c14n.CanonicalizerSpi;
import org.apache.xml.security.transforms.TransformSpi;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageSecurityProviderBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.util.ServiceUtil;

class XmlsecProcessor {

    private static final String FEATURE = "xmlsec";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(FEATURE);
    }

    @BuildStep
    IndexDependencyBuildItem indexDependencies() {
        return new IndexDependencyBuildItem("org.apache.santuario", "xmlsec");
    }

    @BuildStep
    void registerForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();

        Stream.of(
                SignatureAlgorithmSpi.class.getName(),
                CanonicalizerSpi.class.getName(),
                TransformSpi.class.getName(),
                org.apache.xml.security.stax.securityToken.SecurityTokenFactory.class.getName())
                .flatMap(className -> index.getAllKnownSubclasses(DotName.createSimple(className)).stream())
                .map(classInfo -> classInfo.name().toString())
                .map(className -> ReflectiveClassBuildItem.builder(className).build())
                .forEach(reflectiveClass::produce);

        Stream.of(
                org.apache.xml.security.stax.ext.ResourceResolverLookup.class.getName(),
                org.apache.xml.security.stax.ext.Transformer.class.getName())
                .flatMap(className -> index.getAllKnownImplementors(DotName.createSimple(className)).stream())
                .map(classInfo -> classInfo.name().toString())
                .map(className -> ReflectiveClassBuildItem.builder(className).build())
                .forEach(reflectiveClass::produce);

        reflectiveClass.produce(ReflectiveClassBuildItem.builder(
                GCMParameterSpec.class.getName(), XPathType[].class.getName()).build());
    }

    @BuildStep
    void runtimeInitializedClass(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClass) {
        runtimeInitializedClass
                .produce(new RuntimeInitializedClassBuildItem("org.apache.xml.security.stax.impl.InboundSecurityContextImpl"));
    }

    @BuildStep
    void nativeImageResources(BuildProducer<NativeImageResourceBuildItem> nativeImageResources) {
        Stream.of(
                "bindings/bindings.cat",
                "bindings/c14n.xjb",
                "bindings/dsig.xjb",
                "bindings/dsig11.xjb",
                "bindings/rsa-pss.xjb",
                "bindings/security-config.xjb",
                "bindings/xenc.xjb",
                "bindings/xenc11.xjb",
                "bindings/xop.xjb",
                "bindings/schemas/datatypes.dtd",
                "bindings/schemas/exc-c14n.xsd",
                "bindings/schemas/rsa-pss.xsd",
                "bindings/schemas/xenc-schema.xsd",
                "bindings/schemas/xenc-schema-11.xsd",
                "bindings/schemas/xml.xsd",
                "bindings/schemas/xmldsig11-schema.xsd",
                "bindings/schemas/xmldsig-core-schema.xsd",
                "bindings/schemas/XMLSchema.dtd",
                "bindings/schemas/xop-include.xsd",
                "schemas/security-config.xsd",
                "security-config.xml")
                .map(NativeImageResourceBuildItem::new)
                .forEach(nativeImageResources::produce);
    }

    @BuildStep
    NativeImageSecurityProviderBuildItem saslSecurityProvider() {
        return new NativeImageSecurityProviderBuildItem(XMLDSigRI.class.getName());
    }

    @BuildStep
    void resourceBundle(BuildProducer<NativeImageResourceBundleBuildItem> resourceBundle) {
        resourceBundle.produce(
                new NativeImageResourceBundleBuildItem("org.apache.xml.security.resource.xmlsecurity"));
    }

    @BuildStep
    void serviceProviders(BuildProducer<ServiceProviderBuildItem> serviceProviders) {
        Stream.of(
                javax.xml.validation.SchemaFactory.class)
                .map(Class::getName)
                .forEach(serviceName -> {
                    try {
                        final Set<String> names = ServiceUtil.classNamesNamedIn(Thread.currentThread().getContextClassLoader(),
                                ServiceProviderBuildItem.SPI_ROOT + serviceName);
                        serviceProviders.produce(new ServiceProviderBuildItem(serviceName, new ArrayList<>(names)));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

}
