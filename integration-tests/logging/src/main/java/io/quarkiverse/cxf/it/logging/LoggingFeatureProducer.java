package io.quarkiverse.cxf.it.logging;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

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
