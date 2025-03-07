package io.quarkiverse.cxf.ws.security.deployment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Stream;

import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.opensaml.xmlsec.signature.impl.X509CRLImpl;
import org.opensaml.xmlsec.signature.impl.X509CertificateImpl;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.util.ServiceUtil;

/**
 * {@link BuildStep}s related to {@code org.opensaml:opensaml*} artifacts.
 */
public class OpenSamlProcessor {

    @BuildStep
    void indexDependencies(BuildProducer<IndexDependencyBuildItem> indexDependencies) {
        Stream.of(
                "org.opensaml:opensaml-core-api",
                "org.opensaml:opensaml-core-impl",
                "org.opensaml:opensaml-profile-api",
                "org.opensaml:opensaml-saml-api",
                "org.opensaml:opensaml-saml-impl",
                "org.opensaml:opensaml-security-impl",
                "org.opensaml:opensaml-security-api",
                "org.opensaml:opensaml-soap-api",
                "org.opensaml:opensaml-xacml-impl",
                "org.opensaml:opensaml-xacml-api",
                "org.opensaml:opensaml-xacml-saml-api",
                "org.opensaml:opensaml-xacml-saml-impl",
                "org.opensaml:opensaml-xmlsec-api",
                "org.opensaml:opensaml-xmlsec-impl")
                .forEach(ga -> {
                    String[] coords = ga.split(":");
                    indexDependencies.produce(new IndexDependencyBuildItem(coords[0], coords[1]));
                });
    }

    @BuildStep
    NativeImageResourceBuildItem nativeImageResource() {
        return new NativeImageResourceBuildItem(
                "opensaml-config.properties",

                // from org.apache.wss4j.common.saml.OpenSAMLBootstrap
                "default-config.xml",
                "schema-config.xml",
                "saml1-assertion-config.xml",
                "saml1-metadata-config.xml",
                "saml1-protocol-config.xml",
                "saml2-assertion-config.xml",
                "saml2-assertion-delegation-restriction-config.xml",
                "saml2-ecp-config.xml",
                "saml2-metadata-algorithm-config.xml",
                "saml2-metadata-attr-config.xml",
                "saml2-metadata-config.xml",
                "saml2-metadata-idp-discovery-config.xml",
                "saml2-metadata-query-config.xml",
                "saml2-metadata-reqinit-config.xml",
                "saml2-metadata-ui-config.xml",
                "saml2-metadata-rpi-config.xml",
                "saml2-protocol-config.xml",
                "saml2-protocol-thirdparty-config.xml",
                "saml2-protocol-aslo-config.xml",
                "saml2-channel-binding-config.xml",
                "saml-ec-gss-config.xml",
                "signature-config.xml",
                "wss4j-signature-config.xml",
                "encryption-config.xml",
                "xacml20-context-config.xml",
                "xacml20-policy-config.xml",
                "xacml10-saml2-profile-config.xml",
                "xacml11-saml2-profile-config.xml",
                "xacml2-saml2-profile-config.xml",
                "xacml3-saml2-profile-config.xml",
                "saml2-xacml2-profile.xml",

                "schema/xmltooling-config.xsd",
                "schema/datatypes.dtd",
                "schema/xml.xsd",
                "schema/XMLSchema.dtd",
                "schema/XMLSchema.xsd",
                "schema/xmltooling-config.xsd");
    }

    @BuildStep
    void reflectiveClasses(
            CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {

        final IndexView index = combinedIndexBuildItem.getIndex();
        Stream.of(
                "org.opensaml.core.xml.XMLObjectBuilder",
                "org.opensaml.core.xml.io.Marshaller",
                "org.opensaml.core.xml.io.Unmarshaller")
                .map(DotName::createSimple)
                .flatMap(dotName -> index.getAllKnownImplementors(dotName).stream())
                .map(classInfo -> classInfo.name().toString())
                .map(className -> ReflectiveClassBuildItem.builder(className).build())
                .forEach(reflectiveClasses::produce);

    }

    @BuildStep
    void registerServices(BuildProducer<ServiceProviderBuildItem> serviceProvider) {
        Stream.of(
                "org.opensaml.core.config.Configuration",
                "org.opensaml.core.config.ConfigurationPropertiesSource",
                "org.opensaml.core.config.Initializer",
                "org.opensaml.xmlsec.signature.support.SignerProvider",
                "org.opensaml.xmlsec.algorithm.AlgorithmDescriptor",
                "org.opensaml.xmlsec.signature.support.SignatureValidationProvider")
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
    void RuntimeInitializedClass(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClass) {
        Stream.of(
                X509CertificateImpl.class.getName(),
                X509CRLImpl.class.getName())
                .map(RuntimeInitializedClassBuildItem::new)
                .forEach(runtimeInitializedClass::produce);
    }

}
