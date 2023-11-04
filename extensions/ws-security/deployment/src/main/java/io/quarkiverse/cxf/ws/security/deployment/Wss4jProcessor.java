package io.quarkiverse.cxf.ws.security.deployment;

import java.util.stream.Stream;

import org.apache.xml.security.stax.ext.XMLSecurityHeaderHandler;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;

/**
 * {@link BuildStep}s related to {@code org.apache.ws.security:wss4j}
 */
public class Wss4jProcessor {
    @BuildStep
    void indexDependencies(BuildProducer<IndexDependencyBuildItem> indexDependencies) {
        Stream.of(
                "org.apache.wss4j:wss4j-bindings",
                "org.apache.wss4j:wss4j-ws-security-common",
                "org.apache.wss4j:wss4j-ws-security-dom",
                "org.apache.wss4j:wss4j-ws-security-stax",
                "org.apache.wss4j:wss4j-ws-security-policy-stax",
                "org.opensaml:opensaml-core",
                "org.opensaml:opensaml-xmlsec-api",
                "org.opensaml:opensaml-xmlsec-impl")
                .forEach(ga -> {
                    String[] coords = ga.split(":");
                    indexDependencies.produce(new IndexDependencyBuildItem(coords[0], coords[1]));
                });
    }

    @BuildStep
    void reflectiveClass(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        final IndexView index = combinedIndexBuildItem.getIndex();
        Stream.of(
                "org.apache.wss4j.dom.action.Action",
                "org.apache.wss4j.dom.processor.Processor",
                "org.apache.wss4j.dom.validate.Validator",
                "org.opensaml.core.xml.io.Unmarshaller",
                XMLSecurityHeaderHandler.class.getName())
                .map(DotName::createSimple)
                .flatMap(dotName -> index.getAllKnownImplementors(dotName).stream())
                .map(classInfo -> classInfo.name().toString())
                .map(className -> ReflectiveClassBuildItem.builder(className).build())
                .forEach(reflectiveClass::produce);

        reflectiveClass.produce(ReflectiveClassBuildItem.builder(org.apache.wss4j.dom.transform.STRTransform.class).build());
    }

    @BuildStep
    void runtimeInitializedClass(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClass) {
        Stream.of(
                "org.apache.wss4j.common.saml.builder.SAML1ComponentBuilder",
                "org.apache.wss4j.common.saml.builder.SAML2ComponentBuilder",
                "org.apache.wss4j.stax.impl.processor.input.DecryptInputProcessor")
                .map(RuntimeInitializedClassBuildItem::new)
                .forEach(runtimeInitializedClass::produce);
    }

    @BuildStep
    void resourceBundle(BuildProducer<NativeImageResourceBundleBuildItem> resourceBundle) {
        resourceBundle.produce(
                new NativeImageResourceBundleBuildItem("messages.wss4j_errors"));
    }

    @BuildStep
    void nativeImageResources(BuildProducer<NativeImageResourceBuildItem> nativeImageResources) {
        Stream.of(
                "wss/wss-config.xml",
                "schemas/datatypes.dtd",
                "schemas/exc-c14n.xsd",
                "schemas/oasis-200401-wss-wssecurity-secext-1.0.xsd",
                "schemas/oasis-200401-wss-wssecurity-utility-1.0.xsd",
                "schemas/oasis-wss-wssecurity-secext-1.1.xsd",
                "schemas/soap-1.1.xsd",
                "schemas/soap-1.2.xsd",
                "schemas/ws-secureconversation-1.3.xsd",
                "schemas/ws-secureconversation-200502.xsd",
                "schemas/xenc-schema.xsd",
                "schemas/xenc-schema-11.xsd",
                "schemas/xml.xsd",
                "schemas/xmldsig11-schema.xsd",
                "schemas/xmldsig-core-schema.xsd",
                "schemas/XMLSchema.dtd",
                "schemas/xop-include.xsd")
                .map(NativeImageResourceBuildItem::new)
                .forEach(nativeImageResources::produce);
    }

    @BuildStep
    void proxies(BuildProducer<NativeImageProxyDefinitionBuildItem> proxies) {
        /*
         * These are actually needed only for Stax security implementation
         * If the enable.streaming config were build time we could make this conditional
         */
        proxies.produce(new NativeImageProxyDefinitionBuildItem(
                org.apache.wss4j.stax.securityToken.X509SecurityToken.class.getName(),
                org.apache.wss4j.stax.securityToken.SubjectAndPrincipalSecurityToken.class.getName(),
                org.apache.xml.security.stax.securityToken.SecurityToken.class.getName(),
                org.apache.xml.security.stax.securityToken.InboundSecurityToken.class.getName()));
        proxies.produce(new NativeImageProxyDefinitionBuildItem(
                org.apache.xml.security.stax.securityToken.InboundSecurityToken.class.getName(),
                org.apache.xml.security.stax.securityToken.SecurityToken.class.getName()));
    }
}
