package io.quarkiverse.cxf.santuario.deployment;

import java.util.stream.Stream;

import javax.crypto.spec.GCMParameterSpec;
import javax.xml.crypto.dsig.spec.XPathType;

import org.apache.jcp.xml.dsig.internal.dom.XMLDSigRI;
import org.apache.xml.security.c14n.CanonicalizerSpi;
import org.apache.xml.security.stax.ext.XMLSecurityConstants;
import org.apache.xml.security.transforms.TransformSpi;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageSecurityProviderBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeReinitializedClassBuildItem;

/**
 * {@link BuildStep}s related to {@code org.apache.santuario:xmlsec}
 */
public class SantuarioProcessor {

    @BuildStep
    IndexDependencyBuildItem indexDependencies() {
        return new IndexDependencyBuildItem("org.apache.santuario", "xmlsec");
    }

    @BuildStep
    void reflectiveClasses(BuildProducer<ReflectiveClassBuildItem> reflectiveClass, CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();

        Stream.of(
                CanonicalizerSpi.class,
                TransformSpi.class)
                .map(aClass -> aClass.getName())
                .map(DotName::createSimple)
                .flatMap(dotName -> index.getAllKnownSubclasses(dotName).stream())
                .map(classInfo -> classInfo.name().toString())
                .map(className -> new ReflectiveClassBuildItem(false, false, className))
                .forEach(reflectiveClass::produce);

        Stream.of(GCMParameterSpec.class.getName(), XPathType[].class.getName())
                .map(className -> new ReflectiveClassBuildItem(false, false, className))
                .forEach(reflectiveClass::produce);
    }

    @BuildStep
    void runtimeReinitializedClasses(BuildProducer<RuntimeReinitializedClassBuildItem> runtimeReinitializedClasses) {
        Stream.of(
                /* XMLSecurityConstants has a SecureRandom field initialized in a static initializer */
                XMLSecurityConstants.class.getName())
                .map(RuntimeReinitializedClassBuildItem::new)
                .forEach(runtimeReinitializedClasses::produce);
    }

    @BuildStep
    void nativeImageSecurityProvider(BuildProducer<NativeImageSecurityProviderBuildItem> nativeImageSecurityProvider) {
        nativeImageSecurityProvider.produce(
                new NativeImageSecurityProviderBuildItem(XMLDSigRI.class.getName()));
    }

    @BuildStep
    void runtimeInitializedClass(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClass) {
        Stream.of("org.apache.xml.security.stax.impl.InboundSecurityContextImpl")
                .map(RuntimeInitializedClassBuildItem::new)
                .forEach(runtimeInitializedClass::produce);
    }

    @BuildStep
    void resourceBundle(BuildProducer<NativeImageResourceBundleBuildItem> resourceBundle) {
        resourceBundle.produce(
                new NativeImageResourceBundleBuildItem("org.apache.xml.security.resource.xmlsecurity"));
    }

}
