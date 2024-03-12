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
         * If `true` or `pretty`, the message logging will be enabled; otherwise it will not be enabled. If the value is
         * `pretty` (since 2.7.0), the `pretty` attribute will effectively be set to `true`. The default is given by
         * `xref:#quarkus-cxf_quarkus-cxf-logging-enabled-for[quarkus.cxf.logging.enabled-for]`.
         *
         * @since 2.6.0
         * @asciidoclet
         */
        Optional<PrettyBoolean> enabled();

        /**
         * If `true`, the XML elements will be indented in the log; otherwise they will appear unindented. The default is given
         * by `xref:#quarkus-cxf_quarkus-cxf-logging-pretty[quarkus.cxf.logging.pretty]`
         *
         * @since 2.6.0
         * @asciidoclet
         */
        Optional<Boolean> pretty();

        /**
         * A message length in bytes at which it is truncated in the log. The default is given by
         * `xref:#quarkus-cxf_quarkus-cxf-logging-limit[quarkus.cxf.logging.limit]`
         *
         * @since 2.6.0
         * @asciidoclet
         */
        OptionalInt limit();

        /**
         * A message length in bytes at which it will be written to disk. `-1` is unlimited. The default is given by
         * `xref:#quarkus-cxf_quarkus-cxf-logging-in-mem-threshold[quarkus.cxf.logging.in-mem-threshold]`
         *
         * @since 2.6.0
         * @asciidoclet
         */
        OptionalLong inMemThreshold();

        /**
         * If `true`, binary payloads will be logged; otherwise they won't be logged. The default is given by
         * `xref:#quarkus-cxf_quarkus-cxf-logging-log-binary[quarkus.cxf.logging.log-binary]`
         *
         * @since 2.6.0
         * @asciidoclet
         */
        Optional<Boolean> logBinary();

        /**
         * If `true`, multipart payloads will be logged; otherwise they won't be logged. The default is given by
         * `xref:#quarkus-cxf_quarkus-cxf-logging-log-multipart[quarkus.cxf.logging.log-multipart]`
         *
         * @since 2.6.0
         * @asciidoclet
         */
        Optional<Boolean> logMultipart();

        /**
         * If `true`, verbose logging will be enabled; otherwise it won't be enabled. The default is given by
         * `xref:#quarkus-cxf_quarkus-cxf-logging-verbose[quarkus.cxf.logging.verbose]`
         *
         * @since 2.6.0
         * @asciidoclet
         */
        Optional<Boolean> verbose();

        /**
         * A comma separated list of additional binary media types to add to the default values in the `LoggingInInterceptor`
         * whose content will not be logged unless `log-binary` is `true`. The default is given by
         * `xref:#quarkus-cxf_quarkus-cxf-logging-in-binary-content-media-types[quarkus.cxf.logging.in-binary-content-media-types]`
         *
         * @since 2.6.0
         * @asciidoclet
         */
        Optional<List<String>> inBinaryContentMediaTypes();

        /**
         * A comma separated list of additional binary media types to add to the default values in the `LoggingOutInterceptor`
         * whose content will not be logged unless `log-binary` is `true`. The default is given by
         * `xref:#quarkus-cxf_quarkus-cxf-logging-out-binary-content-media-types[quarkus.cxf.logging.out-binary-content-media-types]`
         *
         * @since 2.6.0
         * @asciidoclet
         */
        Optional<List<String>> outBinaryContentMediaTypes();

        /**
         * A comma separated list of additional binary media types to add to the default values in the `LoggingOutInterceptor`
         * and `LoggingInInterceptor` whose content will not be logged unless `log-binary` is `true`. The default is given by
         * `xref:#quarkus-cxf_quarkus-cxf-logging-binary-content-media-types[quarkus.cxf.logging.binary-content-media-types]`
         *
         * @since 2.6.0
         * @asciidoclet
         */
        Optional<List<String>> binaryContentMediaTypes();

        /**
         * A comma separated list of XML elements containing sensitive information to be masked in the log. The default is given
         * by `xref:#quarkus-cxf_quarkus-cxf-logging-sensitive-element-names[quarkus.cxf.logging.sensitive-element-names]`
         *
         * @since 2.6.0
         * @asciidoclet
         */
        Optional<Set<String>> sensitiveElementNames();

        /**
         * A comma separated list of protocol headers containing sensitive information to be masked in the log. The default is
         * given by
         * `xref:#quarkus-cxf_quarkus-cxf-logging-sensitive-protocol-header-names[quarkus.cxf.logging.sensitive-protocol-header-names]`
         *
         * @since 2.6.0
         * @asciidoclet
         */
        Optional<Set<String>> sensitiveProtocolHeaderNames();
    }

    @ConfigGroup
    public interface GlobalLoggingConfig {

        /**
         * Specifies whether the message logging will be enabled for clients, services, both or none. This setting can be
         * overridden per client or service endpoint using
         * `xref:#quarkus-cxf_quarkus-cxf-endpoint-endpoint-path-logging-enabled[quarkus.cxf.endpoint."/endpoint-path".logging.enabled]`
         * or
         * `xref:#quarkus-cxf_quarkus-cxf-client-client-name-logging-enabled[quarkus.cxf.client."client-name".logging.enabled]`
         * respectively.
         *
         * @since 2.6.0
         * @asciidoclet
         */
        @WithDefault("none")
        @WithConverter(EnabledForConverter.class)
        EnabledFor enabledFor();

        /**
         * If `true`, the XML elements will be indented in the log; otherwise they will appear unindented. This setting can be
         * overridden per client or service endpoint using
         * `xref:#quarkus-cxf_quarkus-cxf-endpoint-endpoint-path-logging-pretty[quarkus.cxf.endpoint."/endpoint-path".logging.pretty]`
         * or
         * `xref:#quarkus-cxf_quarkus-cxf-client-client-name-logging-pretty[quarkus.cxf.client."client-name".logging.pretty]`
         * respectively.
         *
         * @since 2.6.0
         * @asciidoclet
         */
        @WithDefault("false")
        boolean pretty();

        /**
         * A message length in bytes at which it is truncated in the log. This setting can be overridden per client or service
         * endpoint using
         * `xref:#quarkus-cxf_quarkus-cxf-endpoint-endpoint-path-logging-limit[quarkus.cxf.endpoint."/endpoint-path".logging.limit]`
         * or
         * `xref:#quarkus-cxf_quarkus-cxf-client-client-name-logging-limit[quarkus.cxf.client."client-name".logging.limit]`
         * respectively.
         *
         * @since 2.6.0
         * @asciidoclet
         */
        // 48 kB
        @WithDefault("49152")
        int limit();

        /**
         * A message length in bytes at which it will be written to disk. `-1` is unlimited. This setting can be overridden per
         * client or service endpoint using
         * `xref:#quarkus-cxf_quarkus-cxf-endpoint-endpoint-path-logging-in-mem-threshold[quarkus.cxf.endpoint."/endpoint-path".logging.in-mem-threshold]`
         * or
         * `xref:#quarkus-cxf_quarkus-cxf-client-client-name-logging-in-mem-threshold[quarkus.cxf.client."client-name".logging.in-mem-threshold]`
         * respectively.
         *
         * @since 2.6.0
         * @asciidoclet
         */
        @WithDefault("-1")
        long inMemThreshold();

        /**
         * If `true`, binary payloads will be logged; otherwise they won't be logged. This setting can be overridden per client
         * or service endpoint using
         * `xref:#quarkus-cxf_quarkus-cxf-endpoint-endpoint-path-logging-log-binary[quarkus.cxf.endpoint."/endpoint-path".logging.log-binary]`
         * or
         * `xref:#quarkus-cxf_quarkus-cxf-client-client-name-logging-log-binary[quarkus.cxf.client."client-name".logging.log-binary]`
         * respectively.
         *
         * @since 2.6.0
         * @asciidoclet
         */
        @WithDefault("false")
        boolean logBinary();

        /**
         * If `true`, multipart payloads will be logged; otherwise they won't be logged. This setting can be overridden per
         * client or service endpoint using
         * `xref:#quarkus-cxf_quarkus-cxf-endpoint-endpoint-path-logging-log-multipart[quarkus.cxf.endpoint."/endpoint-path".logging.log-multipart]`
         * or
         * `xref:#quarkus-cxf_quarkus-cxf-client-client-name-logging-log-multipart[quarkus.cxf.client."client-name".logging.log-multipart]`
         * respectively.
         *
         * @since 2.6.0
         * @asciidoclet
         */
        @WithDefault("true")
        boolean logMultipart();

        /**
         * If `true`, verbose logging will be enabled; otherwise it won't be enabled. This setting can be overridden per client
         * or service endpoint using
         * `xref:#quarkus-cxf_quarkus-cxf-endpoint-endpoint-path-logging-verbose[quarkus.cxf.endpoint."/endpoint-path".logging.verbose]`
         * or
         * `xref:#quarkus-cxf_quarkus-cxf-client-client-name-logging-verbose[quarkus.cxf.client."client-name".logging.verbose]`
         * respectively.
         *
         * @since 2.6.0
         * @asciidoclet
         */
        @WithDefault("true")
        boolean verbose();

        /**
         * A comma separated list of additional binary media types to add to the default values in the `LoggingInInterceptor`
         * whose content will not be logged unless `log-binary` is `true`. This setting can be overridden per client or service
         * endpoint using
         * `xref:#quarkus-cxf_quarkus-cxf-endpoint-endpoint-path-logging-in-binary-content-media-types[quarkus.cxf.endpoint."/endpoint-path".logging.in-binary-content-media-types]`
         * or
         * `xref:#quarkus-cxf_quarkus-cxf-client-client-name-logging-in-binary-content-media-types[quarkus.cxf.client."client-name".logging.in-binary-content-media-types]`
         * respectively.
         *
         * @since 2.6.0
         * @asciidoclet
         */
        Optional<List<String>> inBinaryContentMediaTypes();

        /**
         * A comma separated list of additional binary media types to add to the default values in the `LoggingOutInterceptor`
         * whose content will not be logged unless `log-binary` is `true`. This setting can be overridden per client or service
         * endpoint using
         * `xref:#quarkus-cxf_quarkus-cxf-endpoint-endpoint-path-logging-out-binary-content-media-types[quarkus.cxf.endpoint."/endpoint-path".logging.out-binary-content-media-types]`
         * or
         * `xref:#quarkus-cxf_quarkus-cxf-client-client-name-logging-out-binary-content-media-types[quarkus.cxf.client."client-name".logging.out-binary-content-media-types]`
         * respectively.
         *
         * @since 2.6.0
         * @asciidoclet
         */
        Optional<List<String>> outBinaryContentMediaTypes();

        /**
         * A comma separated list of additional binary media types to add to the default values in the `LoggingOutInterceptor`
         * and `LoggingInInterceptor` whose content will not be logged unless `log-binary` is `true`. This setting can be
         * overridden per client or service endpoint using
         * `xref:#quarkus-cxf_quarkus-cxf-endpoint-endpoint-path-logging-binary-content-media-types[quarkus.cxf.endpoint."/endpoint-path".logging.binary-content-media-types]`
         * or
         * `xref:#quarkus-cxf_quarkus-cxf-client-client-name-logging-binary-content-media-types[quarkus.cxf.client."client-name".logging.binary-content-media-types]`
         * respectively.
         *
         * @since 2.6.0
         * @asciidoclet
         */
        Optional<List<String>> binaryContentMediaTypes();

        /**
         * A comma separated list of XML elements containing sensitive information to be masked in the log. This setting can be
         * overridden per client or service endpoint using
         * `xref:#quarkus-cxf_quarkus-cxf-endpoint-endpoint-path-logging-sensitive-element-names[quarkus.cxf.endpoint."/endpoint-path".logging.sensitive-element-names]`
         * or
         * `xref:#quarkus-cxf_quarkus-cxf-client-client-name-logging-sensitive-element-names[quarkus.cxf.client."client-name".logging.sensitive-element-names]`
         * respectively.
         *
         * @since 2.6.0
         * @asciidoclet
         */
        Optional<Set<String>> sensitiveElementNames();

        /**
         * A comma separated list of protocol headers containing sensitive information to be masked in the log. This setting can
         * be overridden per client or service endpoint using
         * `xref:#quarkus-cxf_quarkus-cxf-endpoint-endpoint-path-logging-sensitive-protocol-header-names[quarkus.cxf.endpoint."/endpoint-path".logging.sensitive-protocol-header-names]`
         * or
         * `xref:#quarkus-cxf_quarkus-cxf-client-client-name-logging-sensitive-protocol-header-names[quarkus.cxf.client."client-name".logging.sensitive-protocol-header-names]`
         * respectively.
         *
         * @since 2.6.0
         * @asciidoclet
         */
        Optional<Set<String>> sensitiveProtocolHeaderNames();
    }
}
