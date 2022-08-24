package io.quarkiverse.cxf.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

public class QuarkusCxfWsSecurityProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem("cxf-rt-ws-security");
    }

    @BuildStep
    void registerWsSecurityReflectionItems(BuildProducer<ReflectiveClassBuildItem> reflectiveItems) {

        reflectiveItems.produce(new ReflectiveClassBuildItem(true, false,

                "org.apache.cxf.ws.security.policy.WSSecurityPolicyLoader",
                "org.apache.cxf.ws.security.tokenstore.SecurityToken",
                "org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor",
                "org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor",

                "org.apache.xml.resolver.CatalogManager" // xml-resolver
        ));

        reflectiveItems.produce(new ReflectiveClassBuildItem(true, true,
                "org.apache.cxf.ws.security.cache.CacheCleanupListener"));

    }

}
