package io.quarkiverse.cxf.deployment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.wsdl.extensions.ExtensibilityElement;

import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.util.ServiceUtil;

/**
 * {@link BuildStep}s related to {wsdl4j:wsdl4j}
 */
class Wsdl4jProcessor {

    private static final Logger log = Logger.getLogger(Wsdl4jProcessor.class);

    @BuildStep
    void indexDependencies(BuildProducer<IndexDependencyBuildItem> indexDependencies) {
        Stream.of(
                "wsdl4j:wsdl4j")
                .forEach(ga -> {
                    String[] coords = ga.split(":");
                    indexDependencies.produce(new IndexDependencyBuildItem(coords[0], coords[1]));
                });
    }

    @BuildStep
    void httpProxies(BuildProducer<NativeImageProxyDefinitionBuildItem> proxies) {
        proxies.produce(new NativeImageProxyDefinitionBuildItem("javax.wsdl.extensions.soap.SOAPOperation"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("javax.wsdl.extensions.soap.SOAPBody"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("javax.wsdl.extensions.soap.SOAPHeader"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("javax.wsdl.extensions.soap.SOAPAddress"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("javax.wsdl.extensions.soap.SOAPBinding"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("javax.wsdl.extensions.soap.SOAPFault"));
        proxies.produce(new NativeImageProxyDefinitionBuildItem("javax.wsdl.extensions.soap.SOAPHeaderFault"));
    }

    @BuildStep
    void reflectiveClass(
            CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        final IndexView index = combinedIndexBuildItem.getIndex();

        Stream.of("12", "")
                .map(version -> ReflectiveClassBuildItem.builder(
                        "com.ibm.wsdl.extensions.soap" + version + ".SOAP" + version + "AddressImpl",
                        "com.ibm.wsdl.extensions.soap" + version + ".SOAP" + version + "BindingImpl",
                        "com.ibm.wsdl.extensions.soap" + version + ".SOAP" + version + "BodyImpl",
                        "com.ibm.wsdl.extensions.soap" + version + ".SOAP" + version + "FaultImpl",
                        "com.ibm.wsdl.extensions.soap" + version + ".SOAP" + version + "HeaderImpl",
                        "com.ibm.wsdl.extensions.soap" + version + ".SOAP" + version + "OperationImpl").methods().build())
                .forEach(reflectiveClass::produce);

        reflectiveClass.produce(ReflectiveClassBuildItem.builder("com.ibm.wsdl.extensions.schema.SchemaImpl").build());

        final Set<String> extensibilityElements = index.getAllKnownImplementations(ExtensibilityElement.class)
                .stream()
                .map(classInfo -> classInfo.name().toString())
                .collect(Collectors.toSet());
        log.debugf("Registring ExtensibilityElements for reflection: %s", extensibilityElements);
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(extensibilityElements.toArray(new String[0])).build());
    }

    @BuildStep
    void serviceProviders(BuildProducer<ServiceProviderBuildItem> serviceProvider) {

        Stream.of(
                "javax.wsdl.factory.WSDLFactory")
                .forEach(serviceName -> {
                    try {
                        Set<String> names = ServiceUtil.classNamesNamedIn(Thread.currentThread().getContextClassLoader(),
                                ServiceProviderBuildItem.SPI_ROOT + serviceName);
                        if (names.isEmpty()) {
                            names = Collections.singleton("com.ibm.wsdl.factory.WSDLFactoryImpl");
                        }
                        serviceProvider.produce(new ServiceProviderBuildItem(serviceName, new ArrayList<>(names)));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

    }
}
