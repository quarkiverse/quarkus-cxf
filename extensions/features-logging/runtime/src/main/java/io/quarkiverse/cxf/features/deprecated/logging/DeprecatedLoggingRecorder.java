package io.quarkiverse.cxf.features.deprecated.logging;

import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class DeprecatedLoggingRecorder {
    private static final Logger log = Logger.getLogger(DeprecatedLoggingRecorder.class);

    public void warnAboutDeprecation() {
        log.warn(
                "io.quarkiverse.cxf:quarkus-cxf-rt-features-logging is deprecated and will be removed in the future. Use io.quarkiverse.cxf:quarkus-cxf instead.");
    }
}
