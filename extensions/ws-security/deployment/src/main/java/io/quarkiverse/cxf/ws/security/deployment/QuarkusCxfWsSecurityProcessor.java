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
        return new FeatureBuildItem("cxf-rt-ws-security");
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
