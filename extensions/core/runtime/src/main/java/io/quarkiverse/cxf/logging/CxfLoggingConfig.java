package io.quarkiverse.cxf.logging;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocFilename;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "quarkus.cxf")
@ConfigDocFilename("quarkus-cxf-rt-features-logging.adoc")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface CxfLoggingConfig {

    /**
     * Client configurations.
     */
    @WithName("client")
    Map<String, ClientOrEndpointConfig> clients();

    /**
     * Endpoint configurations.
     */
    @WithName("endpoint")
    Map<String, ClientOrEndpointConfig> endpoints();

    /**
     * A class that provides configurable options of a CXF client.
     */
    @ConfigGroup
    interface ClientOrEndpointConfig {
    }

}
