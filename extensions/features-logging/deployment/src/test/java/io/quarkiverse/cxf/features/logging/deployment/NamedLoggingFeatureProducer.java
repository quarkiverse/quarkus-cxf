package io.quarkiverse.cxf.features.logging.deployment;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;

import org.apache.cxf.ext.logging.LoggingFeature;

public class NamedLoggingFeatureProducer {

    @Produces
    @ApplicationScoped
    @Named("namedLoggingFeature")
    // no @Unremovable annotation here
    LoggingFeature namedLoggingFeature() {
        LoggingFeature loggingFeature = new LoggingFeature();
        loggingFeature.setPrettyLogging(true);
        return loggingFeature;
    }

}
