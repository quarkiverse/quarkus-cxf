package io.quarkiverse.cxf.deployment;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "cxf", phase = ConfigPhase.BUILD_TIME)
public class CxfBuildTimeConfig {
    /**
     * Set this to override the default path for CXF resources
     */
    @ConfigItem(defaultValue = "/")
    String path;

    /**
     * The comma-separated list of wsdl resource path used by cxf
     */
    @ConfigItem
    Optional<List<String>> wsdlPath;
}
