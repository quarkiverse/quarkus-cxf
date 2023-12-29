package io.quarkiverse.cxf;

import java.util.Map;
import java.util.Optional;

import io.quarkiverse.cxf.LoggingConfig.GlobalLoggingConfig;
import io.quarkus.runtime.annotations.ConfigDocIgnore;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefaults;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "quarkus.cxf")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface CxfConfig {

    /**
     * Choose the path of each web services.
     */
    @WithName("endpoint")
    @WithDefaults
    public Map<String, CxfEndpointConfig> endpoints();

    /**
     * Configure client proxies.
     */
    @WithName("client")
    @WithDefaults
    public Map<String, CxfClientConfig> clients();

    /**
     * This exists just as a convenient way to get a {@link CxfClientConfig} with all defaults set.
     * It is not intended to be used by end users.
     */
    public InternalConfig internal();

    /**
     * Global logging related configuration
     */
    GlobalLoggingConfig logging();

    default boolean isClientPresent(String key) {
        return Optional.ofNullable(clients()).map(m -> m.containsKey(key)).orElse(false);
    }

    default CxfClientConfig getClient(String key) {
        return Optional.ofNullable(clients()).map(m -> m.get(key)).orElse(null);
    }

    public interface InternalConfig {
        @ConfigDocIgnore
        public CxfClientConfig client();
    }
}
