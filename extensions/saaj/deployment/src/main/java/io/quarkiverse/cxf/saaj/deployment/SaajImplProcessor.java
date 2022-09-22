package io.quarkiverse.cxf.saaj.deployment;

import java.util.stream.Stream;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;

/**
 * {@link BuildStep}s related to {@code com.sun.xml.messaging.saaj:saaj-impl}
 */
class SaajImplProcessor {

    @BuildStep
    void indexDependencies(BuildProducer<IndexDependencyBuildItem> indexDependencies) {
        Stream.of(
                "com.sun.xml.messaging.saaj:saaj-impl")
                .forEach(ga -> {
                    String[] coords = ga.split(":");
                    indexDependencies.produce(new IndexDependencyBuildItem(coords[0], coords[1]));
                });
    }

    @BuildStep
    void serviceProviders(BuildProducer<ServiceProviderBuildItem> serviceProvider) {
        String[] soapVersions = new String[] { "1_1", "1_2" };
        for (String version : soapVersions) {
            serviceProvider.produce(
                    new ServiceProviderBuildItem(
                            "javax.xml.soap.MessageFactory",
                            "com.sun.xml.messaging.saaj.soap.ver" + version + ".SOAPMessageFactory" + version + "Impl"));

            serviceProvider.produce(
                    new ServiceProviderBuildItem(
                            "javax.xml.soap.SOAPFactory",
                            "com.sun.xml.messaging.saaj.soap.ver" + version + ".SOAPFactory" + version + "Impl"));
        }

        serviceProvider.produce(
                new ServiceProviderBuildItem(
                        "javax.xml.soap.SOAPConnectionFactory",
                        "com.sun.xml.messaging.saaj.client.p2p.HttpSOAPConnectionFactory"));

        serviceProvider.produce(
                new ServiceProviderBuildItem(
                        "javax.xml.soap.SAAJMetaFactory",
                        "com.sun.xml.messaging.saaj.soap.SAAJMetaFactoryImpl"));
    }

}
