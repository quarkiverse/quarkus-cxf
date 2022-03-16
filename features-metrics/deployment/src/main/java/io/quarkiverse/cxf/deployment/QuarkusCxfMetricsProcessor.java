package io.quarkiverse.cxf.deployment;

import javax.xml.ws.WebServiceFeature;

import org.apache.cxf.feature.AbstractPortableFeature;
import org.apache.cxf.feature.DelegatingFeature;
import org.apache.cxf.metrics.MetricsFeature;

import io.quarkiverse.cxf.metrics.QuarkusCxfMetricsFeature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

public class QuarkusCxfMetricsProcessor {

    @BuildStep
    void registerMetricsReflectionItems(BuildProducer<ReflectiveClassBuildItem> reflectiveItems) {
        reflectiveItems.produce(new ReflectiveClassBuildItem(true, false,
                QuarkusCxfMetricsFeature.class.getName(),
                AbstractPortableFeature.class.getName(),
                MetricsFeature.class.getName(),
                DelegatingFeature.class.getName(),
                MetricsFeature.Portable.class.getName(),
                WebServiceFeature.class.getName()));
    }

}
