package io.quarkiverse.cxf;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "quarkus.cxf")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface CxfConfig {

    /**
     * Choose the path of each web services.
     */
    @WithName("endpoint")
    public Map<String, CxfEndpointConfig> endpoints();

    /**
     * Configure client proxies.
     */
    @WithName("client")
    public Map<String, CxfClientConfig> clients();

    default boolean isClientPresent(String key) {
        return Optional.ofNullable(clients()).map(m -> m.containsKey(key)).orElse(false);
    }

    default CxfClientConfig getClient(String key) {
        return Optional.ofNullable(clients()).map(m -> m.get(key)).orElse(null);
    }
}
