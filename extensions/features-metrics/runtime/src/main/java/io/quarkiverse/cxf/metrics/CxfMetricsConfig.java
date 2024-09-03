package io.quarkiverse.cxf.metrics;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkiverse.cxf.EnabledFor;
import io.quarkiverse.cxf.EnabledFor.EnabledForConverter;
import io.quarkus.runtime.annotations.ConfigDocFilename;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
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
@ConfigDocFilename("quarkus-cxf-rt-features-metrics.adoc")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface CxfMetricsConfig {

    /**
     * Global Metrics configuration.
     *
     * @asciidoclet
     */
    GlobalMetricsConfig metrics();

    /**
     * Client configurations.
     *
     * @asciidoclet
     */
    @WithName("client")
    @ConfigDocMapKey("client-name")

    Map<String, ClientsConfig> clients();

    /**
     * Endpoint configurations.
     *
     * @asciidoclet
     */
    @WithName("endpoint")
    @ConfigDocMapKey("/endpoint-path")
    Map<String, EndpointsConfig> endpoints();

    /**
     * Options of CXF clients or service endpoints.
     */
    @ConfigGroup
    interface ClientsConfig {

        /**
         * Metrics related client configuration
         *
         * @asciidoclet
         */
        ClientConfig metrics();

        /**
         * Options of a CXF client.
         */
        @ConfigGroup
        public interface ClientConfig {

            /**
             * If `true` and if `quarkus.cxf.metrics.enabled-for` is set to `both` or `clients` then the `MetricsFeature` will
             * be added to this client; otherwise the feature will not be added to this client.
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
         * Metrics related service endpoint configuration.
         *
         * @asciidoclet
         */
        EndpointConfig metrics();

        /**
         * Options of a CXF service endpoint.
         */
        @ConfigGroup
        public interface EndpointConfig {

            /**
             * If `true` and if `quarkus.cxf.metrics.enabled-for` is set to `both` or `services` then the `MetricsFeature` will
             * be added to this service endpoint; otherwise the feature will not be added to this service endpoint.
             *
             * @since 2.7.0
             * @asciidoclet
             */
            @WithDefault("true")
            boolean enabled();
        }
    }

    @ConfigGroup
    public interface GlobalMetricsConfig {

        /**
         * Specifies whether the metrics collection will be enabled for clients, services, both or none. This global setting can
         * be overridden per client or service endpoint using the `quarkus.cxf.client."client-name".metrics.enabled` or
         * `quarkus.cxf.endpoint."/endpoint-path".metrics.enabled` option respectively.
         *
         * @since 2.7.0
         * @asciidoclet
         */
        @WithDefault("both")
        @WithConverter(EnabledForConverter.class)
        EnabledFor enabledFor();

        /**
         * A list of xref:user-guide/configuration.adoc#beanRefs[references] to
         * `org.apache.cxf.metrics.micrometer.provider.TagsCustomizer` beans
         * that will be attached to the global metrics feature.
         *
         * @since 3.15.0
         * @asciidoclet
         */
        public Optional<List<String>> tagsCustomizers();

    }
}
