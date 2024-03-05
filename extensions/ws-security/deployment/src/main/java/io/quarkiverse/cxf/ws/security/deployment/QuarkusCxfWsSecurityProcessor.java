package io.quarkiverse.cxf.ws.security.deployment;

import java.util.stream.Stream;

import io.quarkiverse.cxf.ws.security.WssFactoryCustomizer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;

public class QuarkusCxfWsSecurityProcessor {

    @BuildStep
    FeatureBuildItem feature() {

        final boolean realBcAvailable = isClassLoadable("org.bouncycastle.LICENSE");
        final boolean bcStubAvailable = isClassLoadable("io.quarkiverse.cxf.ws.security.bc.stub.BcStub");
        if (realBcAvailable && bcStubAvailable) {
            throw new IllegalStateException("Bouncy Castle's org.bouncycastle:bcprov-jdk18on artifact found in dependencies."
                    + " To be able to use it, exclude io.quarkiverse.cxf:quarkus-cxf-bc-stub from"
                    + " io.quarkiverse.cxf:quarkus-cxf-rt-ws-security.");
        }
        if (!realBcAvailable && !bcStubAvailable) {
            throw new IllegalStateException("Neither Bouncy Castle's org.bouncycastle:bcprov-jdk18on"
                    + " nor io.quarkiverse.cxf:quarkus-cxf-bc-stub detected in dependencies."
                    + " For quarkus-cxf-rt-ws-security to work properly, either add io.quarkiverse.cxf:quarkus-cxf-bc-stub (if"
                    + " you do not need Bouncy Castle otherwise) or else add org.bouncycastle:bcprov-jdk18on");
        }
        return new FeatureBuildItem("cxf-rt-ws-security");
    }

    private static boolean isClassLoadable(String cl) {
        try {
            Thread.currentThread().getContextClassLoader().loadClass(cl);
            return true;
        } catch (ClassNotFoundException expected) {
            return false;
        }
    }

    @BuildStep
    void indexDependencies(BuildProducer<IndexDependencyBuildItem> indexDependencies) {
        Stream.of(
                "org.apache.cxf:cxf-rt-ws-security",
                "org.apache.cxf:cxf-rt-security-saml",
                "org.apache.cxf:cxf-rt-security",
                "org.apache.cxf:cxf-rt-ws-mex")
                .forEach(ga -> {
                    String[] coords = ga.split(":");
                    indexDependencies.produce(new IndexDependencyBuildItem(coords[0], coords[1]));
                });
    }

    @BuildStep
    void reflectiveClasses(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {

        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                "org.apache.cxf.ws.security.policy.WSSecurityPolicyLoader",
                "org.apache.cxf.ws.security.tokenstore.SecurityToken",
                "org.apache.xml.resolver.CatalogManager" // xml-resolver
        ).methods().build());

        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                "org.apache.cxf.ws.security.cache.CacheCleanupListener").methods().fields().build());

    }

    @BuildStep
    void runtimeInitializedClass(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClass) {
        Stream.of(
                "org.apache.cxf.rt.security.saml.xacml2.RequestComponentBuilder")
                .map(RuntimeInitializedClassBuildItem::new)
                .forEach(runtimeInitializedClass::produce);
    }

    @BuildStep
    void additionalBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(new AdditionalBeanBuildItem(WssFactoryCustomizer.class));
    }

}
