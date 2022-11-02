package io.quarkiverse.cxf.ws.security.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourcePatternsBuildItem;

/**
 * {@link BuildStep}s related to {@code joda-time:joda-time}.
 */
public class JodaTimeProcessor {

    @BuildStep
    NativeImageResourcePatternsBuildItem nativeImageResource() {
        return NativeImageResourcePatternsBuildItem.builder()
                .includeGlob("org/joda/time/tz/data/**")
                .build();
    }

}
