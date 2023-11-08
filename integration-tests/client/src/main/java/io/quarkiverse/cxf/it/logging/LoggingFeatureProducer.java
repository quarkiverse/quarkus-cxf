package io.quarkiverse.cxf.it.logging;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.apache.cxf.ext.logging.LoggingFeature;

public class LoggingFeatureProducer {

    @Produces
    @ApplicationScoped
    LoggingFeature loggingFeature() {
        LoggingFeature loggingFeature = new LoggingFeature();
        loggingFeature.setPrettyLogging(true); // <1>
        return loggingFeature;
    }

}
