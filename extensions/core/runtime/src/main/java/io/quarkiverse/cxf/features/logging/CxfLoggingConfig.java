package io.quarkiverse.cxf.features.logging;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.quarkus.runtime.annotations.ConfigDocFilename;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
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
        /**
         * WS-Security related client configuration
         */
        LoggingConfig logging();
    }

    /**
     * A class that provides configurable options of a CXF client.
     */
    @ConfigGroup
    public interface LoggingConfig {

        /**
         * If {@code true}, the message logging will be enabled; otherwise it will not be enabled.
         */
        @WithDefault("false")
        boolean enabled();

        /**
         * A message length in bytes at which it is truncated in the log. Default is 48 kB.
         */
        @WithDefault("49152") // 48 kB
        int limit();

        /**
         * A message length in bytes at which it will be written to disk. {@code -1} is unlimited.
         */
        @WithDefault("-1")
        long inMemThreshold();

        /**
         * If {@code true}, the XML elements will be indented in the log; otherwise they will appear unindented.
         */
        @WithDefault("false")
        boolean pretty();

        /**
         * If {@code true}, binary payloads will be logged; otherwise they won't be logged.
         */
        @WithDefault("false")
        boolean logBinary();

        /**
         * If {@code true}, multipart payloads will be logged; otherwise they won't be logged.
         */
        @WithDefault("true")
        boolean logMultipart();

        /**
         * If {@code true}, verbose logging will be enabled; otherwise it won't be enabled.
         */
        @WithDefault("true")
        boolean verbose();

        /**
         * A comma separated list of additional binary media types to the default values in the {@code LoggingInInterceptor}
         * whose content should not be logged.
         */
        Optional<List<String>> inBinaryContentMediaTypes();

        /**
         * A comma separated list of additional binary media types to the default values in the {@code LoggingOutInterceptor}
         * whose content should not be logged.
         */
        Optional<List<String>> outBinaryContentMediaTypes();

        /**
         * A comma separated list of additional binary media types to the default values in both the
         * {@code LoggingOutInterceptor} and {@code LoggingInInterceptor} whose content should not be logged.
         */
        Optional<List<String>> binaryContentMediaTypes();

        /**
         * A comma separated list of XML elements containing sensitive information to be masked in the log.
         */
        Optional<Set<String>> sensitiveElementNames();

        /**
         * A comma separated list of protocol headers containing sensitive information to be masked in the log.
         */
        Optional<Set<String>> sensitiveProtocolHeaderNames();

    }

}
