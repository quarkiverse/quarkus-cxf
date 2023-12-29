package io.quarkiverse.cxf;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;

import io.quarkiverse.cxf.EnabledFor.EnabledForConverter;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;

/**
 * Global, per service endpoint or per client logging options.
 */
@ConfigGroup
public interface LoggingConfig {

    @ConfigGroup
    public interface PerClientOrServiceLoggingConfig {
        /**
         * If {@code true} or {@code pretty}, the message logging will be enabled; otherwise it will not be enabled.
         * If the value is {@code pretty} (since 2.7.0), the {@code pretty} attribute will effectively be set to
         * {@code true}. The default is given by
         * <code><a href="#quarkus-cxf_quarkus.cxf.logging.enabled-for">quarkus.cxf.logging.enabled-for</a></code>.
         *
         * @since 2.6.0
         */
        Optional<PrettyBoolean> enabled();

        /**
         * If {@code true}, the XML elements will be indented in the log; otherwise they will appear unindented.
         * The default is given by
         * <code><a href="#quarkus-cxf_quarkus.cxf.logging.pretty">quarkus.cxf.logging.pretty</a></code>
         *
         * @since 2.6.0
         */
        Optional<Boolean> pretty();

        /**
         * A message length in bytes at which it is truncated in the log.
         * The default is given by
         * <code><a href="#quarkus-cxf_quarkus.cxf.logging.limit">quarkus.cxf.logging.limit</a></code>
         *
         * @since 2.6.0
         */
        OptionalInt limit();

        /**
         * A message length in bytes at which it will be written to disk. {@code -1} is unlimited.
         * The default is given by
         * <code><a href="#quarkus-cxf_quarkus.cxf.logging.in-mem-threshold">quarkus.cxf.logging.in-mem-threshold</a></code>
         *
         * @since 2.6.0
         */
        OptionalLong inMemThreshold();

        /**
         * If {@code true}, binary payloads will be logged; otherwise they won't be logged.
         * The default is given by
         * <code><a href="#quarkus-cxf_quarkus.cxf.logging.log-binary">quarkus.cxf.logging.log-binary</a></code>
         *
         * @since 2.6.0
         */
        Optional<Boolean> logBinary();

        /**
         * If {@code true}, multipart payloads will be logged; otherwise they won't be logged.
         * The default is given by
         * <code><a href="#quarkus-cxf_quarkus.cxf.logging.log-multipart">quarkus.cxf.logging.log-multipart</a></code>
         *
         * @since 2.6.0
         */
        Optional<Boolean> logMultipart();

        /**
         * If {@code true}, verbose logging will be enabled; otherwise it won't be enabled.
         * The default is given by
         * <code><a href="#quarkus-cxf_quarkus.cxf.logging.verbose">quarkus.cxf.logging.verbose</a></code>
         *
         * @since 2.6.0
         */
        Optional<Boolean> verbose();

        /**
         * A comma separated list of additional binary media types to add to the default values in the
         * {@code LoggingInInterceptor} whose content will not be logged unless {@code log-binary} is {@code true}.
         * The default is given by
         * <code><a href=
         * "#quarkus-cxf_quarkus.cxf.logging.in-binary-content-media-types">quarkus.cxf.logging.in-binary-content-media-types</a></code>
         *
         * @since 2.6.0
         */
        Optional<List<String>> inBinaryContentMediaTypes();

        /**
         * A comma separated list of additional binary media types to add to the default values in the
         * {@code LoggingOutInterceptor} whose content will not be logged unless {@code log-binary} is {@code true}.
         * The default is given by
         * <code><a href=
         * "#quarkus-cxf_quarkus.cxf.logging.out-binary-content-media-types">quarkus.cxf.logging.out-binary-content-media-types</a></code>
         *
         * @since 2.6.0
         */
        Optional<List<String>> outBinaryContentMediaTypes();

        /**
         * A comma separated list of additional binary media types to add to the default values in the
         * {@code LoggingOutInterceptor} and {@code LoggingInInterceptor} whose content will not be logged unless
         * {@code log-binary} is {@code true}.
         * The default is given by
         * <code><a href=
         * "#quarkus-cxf_quarkus.cxf.logging.binary-content-media-types">quarkus.cxf.logging.binary-content-media-types</a></code>
         *
         * @since 2.6.0
         */
        Optional<List<String>> binaryContentMediaTypes();

        /**
         * A comma separated list of XML elements containing sensitive information to be masked in the log.
         * The default is given by
         * <code><a href=
         * "#quarkus-cxf_quarkus.cxf.logging.sensitive-element-names">quarkus.cxf.logging.sensitive-element-names</a></code>
         *
         * @since 2.6.0
         */
        Optional<Set<String>> sensitiveElementNames();

        /**
         * A comma separated list of protocol headers containing sensitive information to be masked in the log.
         * The default is given by
         * <code><a href=
         * "#quarkus-cxf_quarkus.cxf.logging.sensitive-protocol-header-names">quarkus.cxf.logging.sensitive-protocol-header-names</a></code>
         *
         * @since 2.6.0
         */
        Optional<Set<String>> sensitiveProtocolHeaderNames();

    }

    @ConfigGroup
    public interface GlobalLoggingConfig {
        /**
         * Specifies whether the message logging will be enabled for clients, services, both or none.
         * This setting can be overridden per client or service endpoint using
         * <code><a href=
         * "#quarkus-cxf_quarkus.cxf.endpoint.-endpoints-.logging.enabled">quarkus.cxf.endpoint."endpoints".logging.enabled</a></code>
         * or
         * <code><a href=
         * "#quarkus-cxf_quarkus.cxf.client.-clients-.logging.enabled">quarkus.cxf.client."clients".logging.enabled</a></code>
         * respectively.
         *
         * @since 2.6.0
         */
        @WithDefault("none")
        @WithConverter(EnabledForConverter.class)
        EnabledFor enabledFor();

        /**
         * If {@code true}, the XML elements will be indented in the log; otherwise they will appear unindented.
         * This setting can be overridden per client or service endpoint using
         * <code><a href=
         * "#quarkus-cxf_quarkus.cxf.endpoint.-endpoints-.logging.pretty">quarkus.cxf.endpoint."endpoints".logging.pretty</a></code>
         * or
         * <code><a href=
         * "#quarkus-cxf_quarkus.cxf.client.-clients-.logging.pretty">quarkus.cxf.client."clients".logging.pretty</a></code>
         * respectively.
         *
         * @since 2.6.0
         */
        @WithDefault("false")
        boolean pretty();

        /**
         * A message length in bytes at which it is truncated in the log.
         * This setting can be overridden per client or service endpoint using
         * <code><a href=
         * "#quarkus-cxf_quarkus.cxf.endpoint.-endpoints-.logging.limit">quarkus.cxf.endpoint."endpoints".logging.limit</a></code>
         * or
         * <code><a href=
         * "#quarkus-cxf_quarkus.cxf.client.-clients-.logging.limit">quarkus.cxf.client."clients".logging.limit</a></code>
         * respectively.
         *
         * @since 2.6.0
         */
        @WithDefault("49152") // 48 kB
        int limit();

        /**
         * A message length in bytes at which it will be written to disk. {@code -1} is unlimited.
         * This setting can be overridden per client or service endpoint using
         * <code><a href=
         * "#quarkus-cxf_quarkus.cxf.endpoint.-endpoints-.logging.in-mem-threshold">quarkus.cxf.endpoint."endpoints".logging.in-mem-threshold</a></code>
         * or
         * <code><a href=
         * "#quarkus-cxf_quarkus.cxf.client.-clients-.logging.in-mem-threshold">quarkus.cxf.client."clients".logging.in-mem-threshold</a></code>
         * respectively.
         *
         * @since 2.6.0
         */
        @WithDefault("-1")
        long inMemThreshold();

        /**
         * If {@code true}, binary payloads will be logged; otherwise they won't be logged.
         * This setting can be overridden per client or service endpoint using
         * <code><a href=
         * "#quarkus-cxf_quarkus.cxf.endpoint.-endpoints-.logging.log-binary">quarkus.cxf.endpoint."endpoints".logging.log-binary</a></code>
         * or
         * <code><a href=
         * "#quarkus-cxf_quarkus.cxf.client.-clients-.logging.log-binary">quarkus.cxf.client."clients".logging.log-binary</a></code>
         * respectively.
         *
         * @since 2.6.0
         */
        @WithDefault("false")
        boolean logBinary();

        /**
         * If {@code true}, multipart payloads will be logged; otherwise they won't be logged.
         * This setting can be overridden per client or service endpoint using
         * <code><a href=
         * "#quarkus-cxf_quarkus.cxf.endpoint.-endpoints-.logging.log-multipart">quarkus.cxf.endpoint."endpoints".logging.log-multipart</a></code>
         * or
         * <code><a href=
         * "#quarkus-cxf_quarkus.cxf.client.-clients-.logging.log-multipart">quarkus.cxf.client."clients".logging.log-multipart</a></code>
         * respectively.
         *
         * @since 2.6.0
         */
        @WithDefault("true")
        boolean logMultipart();

        /**
         * If {@code true}, verbose logging will be enabled; otherwise it won't be enabled.
         * This setting can be overridden per client or service endpoint using
         * <code><a href=
         * "#quarkus-cxf_quarkus.cxf.endpoint.-endpoints-.logging.verbose">quarkus.cxf.endpoint."endpoints".logging.verbose</a></code>
         * or
         * <code><a href=
         * "#quarkus-cxf_quarkus.cxf.client.-clients-.logging.verbose">quarkus.cxf.client."clients".logging.verbose</a></code>
         * respectively.
         *
         * @since 2.6.0
         */
        @WithDefault("true")
        boolean verbose();

        /**
         * A comma separated list of additional binary media types to add to the default values in the
         * {@code LoggingInInterceptor} whose content will not be logged unless {@code log-binary} is {@code true}.
         * This setting can be overridden per client or service endpoint using
         * <code><a href=
         * "#quarkus-cxf_quarkus.cxf.endpoint.-endpoints-.logging.in-binary-content-media-types">quarkus.cxf.endpoint."endpoints".logging.in-binary-content-media-types</a></code>
         * or
         * <code><a href=
         * "#quarkus-cxf_quarkus.cxf.client.-clients-.logging.in-binary-content-media-types">quarkus.cxf.client."clients".logging.in-binary-content-media-types</a></code>
         * respectively.
         *
         * @since 2.6.0
         */
        Optional<List<String>> inBinaryContentMediaTypes();

        /**
         * A comma separated list of additional binary media types to add to the default values in the
         * {@code LoggingOutInterceptor} whose content will not be logged unless {@code log-binary} is {@code true}.
         * This setting can be overridden per client or service endpoint using
         * <code><a href=
         * "#quarkus-cxf_quarkus.cxf.endpoint.-endpoints-.logging.out-binary-content-media-types">quarkus.cxf.endpoint."endpoints".logging.out-binary-content-media-types</a></code>
         * or
         * <code><a href=
         * "#quarkus-cxf_quarkus.cxf.client.-clients-.logging.out-binary-content-media-types">quarkus.cxf.client."clients".logging.out-binary-content-media-types</a></code>
         * respectively.
         *
         * @since 2.6.0
         */
        Optional<List<String>> outBinaryContentMediaTypes();

        /**
         * A comma separated list of additional binary media types to add to the default values in the
         * {@code LoggingOutInterceptor} and {@code LoggingInInterceptor} whose content will not be logged unless
         * {@code log-binary} is {@code true}.
         * This setting can be overridden per client or service endpoint using
         * <code><a href=
         * "#quarkus-cxf_quarkus.cxf.endpoint.-endpoints-.logging.binary-content-media-types">quarkus.cxf.endpoint."endpoints".logging.binary-content-media-types</a></code>
         * or
         * <code><a href=
         * "#quarkus-cxf_quarkus.cxf.client.-clients-.logging.binary-content-media-types">quarkus.cxf.client."clients".logging.binary-content-media-types</a></code>
         * respectively.
         *
         * @since 2.6.0
         */
        Optional<List<String>> binaryContentMediaTypes();

        /**
         * A comma separated list of XML elements containing sensitive information to be masked in the log.
         * This setting can be overridden per client or service endpoint using
         * <code><a href=
         * "#quarkus-cxf_quarkus.cxf.endpoint.-endpoints-.logging.sensitive-element-names">quarkus.cxf.endpoint."endpoints".logging.sensitive-element-names</a></code>
         * or
         * <code><a href=
         * "#quarkus-cxf_quarkus.cxf.client.-clients-.logging.sensitive-element-names">quarkus.cxf.client."clients".logging.sensitive-element-names</a></code>
         * respectively.
         *
         * @since 2.6.0
         */
        Optional<Set<String>> sensitiveElementNames();

        /**
         * A comma separated list of protocol headers containing sensitive information to be masked in the log.
         * This setting can be overridden per client or service endpoint using
         * <code><a href=
         * "#quarkus-cxf_quarkus.cxf.endpoint.-endpoints-.logging.sensitive-protocol-header-names">quarkus.cxf.endpoint."endpoints".logging.sensitive-protocol-header-names</a></code>
         * or
         * <code><a href=
         * "#quarkus-cxf_quarkus.cxf.client.-clients-.logging.sensitive-protocol-header-names">quarkus.cxf.client."clients".logging.sensitive-protocol-header-names</a></code>
         * respectively.
         *
         * @since 2.6.0
         */
        Optional<Set<String>> sensitiveProtocolHeaderNames();

    }

}