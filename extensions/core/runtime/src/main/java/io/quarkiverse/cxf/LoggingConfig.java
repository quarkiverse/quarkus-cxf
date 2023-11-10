package io.quarkiverse.cxf;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

/**
 * Global, per service endpoint or per client logging options.
 */
@ConfigGroup
public interface LoggingConfig {

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

    /**
     * We moved {@link #enabled()} into a separate interface so that it is listed before the other logging options.
     */
    @ConfigGroup
    public interface PerClientOrServiceInternal {
        /**
         * If {@code true}, the message logging will be enabled; otherwise it will not be enabled. Default is {@code false}.
         */
        Optional<Boolean> enabled();
    }

    @ConfigGroup
    public interface PerClientOrServiceLoggingConfig extends PerClientOrServiceInternal, LoggingConfig {
    }

    /**
     * We moved {@link #enabledFor()} into a separate interface so that it is listed before the other logging options.
     */
    @ConfigGroup
    public interface GlobalInternal {
        /**
         * Specifies whether the message logging will be enabled for clients, services, both or none.
         */
        @WithDefault("none")
        EnabledFor enabledFor();
    }

    @ConfigGroup
    public interface GlobalLoggingConfig extends GlobalInternal, LoggingConfig {
    }

}