package io.quarkiverse.cxf;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Quarkus CXF build time configuration options that are also available at runtime but only in read-only mode.
 */
@ConfigRoot(name = "cxf", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class CxfFixedConfig {

    /**
     * The build time part of the client configuration.
     */
    @ConfigItem(name = "client")
    public Map<String, ClientFixedConfig> clients;

    @ConfigGroup
    public static class ClientFixedConfig {

        /**
         * The client service interface class name
         */
        @ConfigItem
        public Optional<String> serviceInterface;

        /**
         * Indicates whether this is an alternative proxy client configuration. If
         * true, then this configuration is ignored when configuring a client without
         * annotation `@CXFClient`.
         */
        @ConfigItem(defaultValue = "false")
        public boolean alternative;

        public static ClientFixedConfig createDefault() {
            ClientFixedConfig result = new ClientFixedConfig();
            result.serviceInterface = Optional.empty();
            return result;
        }
    }
}
