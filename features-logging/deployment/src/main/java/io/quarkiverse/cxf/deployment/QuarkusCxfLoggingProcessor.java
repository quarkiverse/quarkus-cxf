package io.quarkiverse.cxf.deployment;

import javax.xml.ws.WebServiceFeature;

import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.feature.AbstractPortableFeature;
import org.apache.cxf.feature.DelegatingFeature;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

public class QuarkusCxfLoggingProcessor {

    @BuildStep
    void registerLoggingReflectionItems(BuildProducer<ReflectiveClassBuildItem> reflectiveItems) {
        reflectiveItems.produce(new ReflectiveClassBuildItem(true, false,
                LoggingFeature.class.getName(),
                AbstractPortableFeature.class.getName(),
                DelegatingFeature.class.getName(),
                LoggingFeature.Portable.class.getName(),
                WebServiceFeature.class.getName(),
                LoggingInInterceptor.class.getName(),
                LoggingOutInterceptor.class.getName()));
    }

}
