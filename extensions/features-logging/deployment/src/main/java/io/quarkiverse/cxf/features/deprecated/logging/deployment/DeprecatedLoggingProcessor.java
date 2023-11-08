package io.quarkiverse.cxf.features.deprecated.logging.deployment;

import io.quarkiverse.cxf.features.deprecated.logging.DeprecatedLoggingRecorder;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;

public class DeprecatedLoggingProcessor {

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void warnAboutDeprecation(DeprecatedLoggingRecorder recorder) {
        /* Warn at build time */
        new DeprecatedLoggingRecorder().warnAboutDeprecation();
        /* Warn when starting the application */
        recorder.warnAboutDeprecation();
    }

}
