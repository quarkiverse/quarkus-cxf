package io.quarkiverse.cxf.it.ws.rm.server;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

import org.apache.cxf.ext.logging.LoggingFeature;

public class PrettyLoggerProducer {
    @Produces
    @Named
    LoggingFeature prettyLogger() {
        final LoggingFeature result = new LoggingFeature();
        result.setPrettyLogging(true);
        return result;
    }
}
