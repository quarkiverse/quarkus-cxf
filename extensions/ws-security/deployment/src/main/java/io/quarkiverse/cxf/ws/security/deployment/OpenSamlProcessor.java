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
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.util.ServiceUtil;

/**
 * {@link BuildStep}s related to {@code org.opensaml:opensaml*} artifacts.
 */
public class OpenSamlProcessor {

    @BuildStep
    void indexDependencies(BuildProducer<IndexDependencyBuildItem> indexDependencies) {
        Stream.of(
                "org.opensaml:opensaml-core",
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
                .map(className -> new ReflectiveClassBuildItem(false, false, className))
                .forEach(reflectiveClasses::produce);

    }

    @BuildStep
    void registerServices(BuildProducer<ServiceProviderBuildItem> serviceProvider) {
        Stream.of(
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

}
