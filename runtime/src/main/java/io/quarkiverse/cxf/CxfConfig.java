package io.quarkiverse.cxf;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = CxfConfig.CONFIG_NAME, phase = ConfigPhase.RUN_TIME)
public class CxfConfig {
    public static final String CONFIG_NAME = "cxf";

    /**
     * Choose the path of each web services.
     */
    @ConfigItem(name = "endpoint")
    public Map<String, CxfEndpointConfig> endpoints;
}
