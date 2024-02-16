package io.quarkiverse.cxf.opentelemetry;

import java.util.Map;

import io.quarkiverse.cxf.EnabledFor;
import io.quarkiverse.cxf.EnabledFor.EnabledForConverter;
import io.quarkus.runtime.annotations.ConfigDocFilename;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * @since 2.7.0
 */
@ConfigMapping(prefix = "quarkus.cxf")
@ConfigDocFilename("quarkus-cxf-integration-tracing-opentelemetry.adoc")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface CxfOpenTelemetryConfig {

    /**
     * Global OpenTelemetry configuration.
     *
     * @asciidoclet
     */
    GlobalOtelConfig otel();

    /**
     * Client configurations.
     *
     * @asciidoclet
     */
    @WithName("client")
    Map<String, ClientsConfig> clients();

    /**
     * Endpoint configurations.
     *
     * @asciidoclet
     */
    @WithName("endpoint")
    Map<String, EndpointsConfig> endpoints();

    /**
     * Options of CXF clients or service endpoints.
     */
    @ConfigGroup
    interface ClientsConfig {

        /**
         * OpenTelemetry related client configuration
         *
         * @asciidoclet
         */
        ClientConfig otel();

        /**
         * Options of a CXF client.
         */
        @ConfigGroup
        public interface ClientConfig {

            /**
             * If `true` and if `quarkus.cxf.otel.enabled-for` is set to `both` or `clients` then the
             * `OpenTelemetryClientFeature` will be added to this client; otherwise the feature will not be added to this
             * client.
             *
             * @since 2.7.0
             * @asciidoclet
             */
            @WithDefault("true")
            boolean enabled();
        }
    }

    /**
     * Options of CXF clients or service endpoints.
     */
    @ConfigGroup
    interface EndpointsConfig {

        /**
         * OpenTelemetry related service endpoint configuration.
         *
         * @asciidoclet
         */
        EndpointConfig otel();

        /**
         * Options of a CXF service endpoint.
         */
        @ConfigGroup
        public interface EndpointConfig {

            /**
             * If `true` and if `quarkus.cxf.otel.enabled-for` is set to `both` or `services` then the `OpenTelemetryFeature`
             * will be added to this service endpoint; otherwise the feature will not be added to this service endpoint.
             *
             * @since 2.7.0
             * @asciidoclet
             */
            @WithDefault("true")
            boolean enabled();
        }
    }

    @ConfigGroup
    public interface GlobalOtelConfig {

        /**
         * Specifies whether the OpenTelemetry tracing will be enabled for clients, services, both or none. This global setting
         * can be overridden per client or service endpoint using the `quarkus.cxf.client."clients".otel.enabled` or
         * `quarkus.cxf.endpoint."endpoints".otel.enabled` option respectively.
         *
         * @since 2.7.0
         * @asciidoclet
         */
        @WithDefault("both")
        @WithConverter(EnabledForConverter.class)
        EnabledFor enabledFor();
    }
}
