package io.quarkiverse.cxf.ws.rm;

import java.util.Map;
import java.util.Optional;

import org.apache.cxf.ws.addressing.VersionTransformer.Names200408;
import org.apache.cxf.ws.rm.RM10Constants;
import org.apache.cxf.ws.rm.RetransmissionQueue;
import org.apache.cxf.ws.rm.persistence.RMStore;

import io.quarkus.runtime.annotations.ConfigDocFilename;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * @since 2.7.0
 */
@ConfigMapping(prefix = "quarkus.cxf")
@ConfigDocFilename("quarkus-cxf-rt-ws-rm.adoc")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface CxfWsRmConfig {

    /**
     * Global WS-RM configuration.
     *
     * @asciidoclet
     */
    GlobalRmConfig rm();

    /**
     * Client configurations.
     *
     * @asciidoclet
     */
    @WithName("client")
    @ConfigDocMapKey("client-name")
    Map<String, ClientsOrEndpointsConfig> clients();

    /**
     * Endpoint configurations.
     *
     * @asciidoclet
     */
    @WithName("endpoint")
    @ConfigDocMapKey("/endpoint-path")
    Map<String, ClientsOrEndpointsConfig> endpoints();

    @ConfigGroup
    public interface GlobalRmConfig {

        /**
         * WS-RM version namespace: `http://schemas.xmlsoap.org/ws/2005/02/rm/` or
         * `http://docs.oasis-open.org/ws-rx/wsrm/200702`
         *
         * @since 2.7.0
         * @asciidoclet
         */
        @WithDefault(RM10Constants.NAMESPACE_URI)
        String namespace();

        /**
         * WS-Addressing version namespace: `http://schemas.xmlsoap.org/ws/2004/08/addressing` or
         * `http://www.w3.org/2005/08/addressing`. Note that this property is ignored unless you are using the
         * `http://schemas.xmlsoap.org/ws/2005/02/rm/` RM namespace.
         *
         * @since 2.7.0
         * @asciidoclet
         */
        @WithDefault(Names200408.WSA_NAMESPACE_NAME)
        String wsaNamespace();

        /**
         * A time duration in milliseconds after which the associated sequence will be closed if no messages (including
         * acknowledgments and other control messages) were exchanged between the sender and receiver during that period of
         * time. If not set, the associated sequence will never be closed due to inactivity.
         *
         * @since 2.7.0
         * @asciidoclet
         */
        Optional<Long> inactivityTimeout();

        /**
         * A time duration in milliseconds between successive attempts to resend a message that has not been acknowledged by the
         * receiver.
         *
         * @since 2.7.0
         * @asciidoclet
         */
        @WithDefault(RetransmissionQueue.DEFAULT_BASE_RETRANSMISSION_INTERVAL)
        long retransmissionInterval();

        /**
         * If `true` the retransmission interval will be doubled on every transmission attempt; otherwise the retransmission
         * interval stays equal to `quarkus.cxf.rm.retransmission-interval` for every retransmission attempt.
         *
         * @since 2.7.0
         * @asciidoclet
         */
        @WithDefault("false")
        boolean exponentialBackoff();

        /**
         * A time duration in milliseconds within which an acknowledgement for a received message is expected to be sent by a RM
         * destination. If not specified, the acknowledgements will be sent immediately.
         *
         * @since 2.7.0
         * @asciidoclet
         */
        Optional<Long> acknowledgementInterval();

        /**
         * A xref:user-guide/configuration.adoc#beanRefs[reference] to a `org.apache.cxf.ws.rm.persistence.RMStore` bean used to
         * store source and destination sequences and message references.
         *
         * @since 2.7.0
         * @asciidoclet
         */
        Optional<String> store();

        /**
         * A xref:user-guide/configuration.adoc#beanRefs[reference] to a `org.apache.cxf.ws.rm.feature.RMFeature` bean to set on
         * clients and service endpoint which have `quarkus.cxf.++[++client++\|++service++]++."name".rm.enabled = true`.
         *
         * If the value is `++#++defaultRmFeature` then Quarkus CXF creates and configures the bean for you.
         *
         * @since 2.7.0
         * @asciidoclet
         */
        @WithDefault(DefaultRmFeatureProducer.DEFAULT_RM_FEATURE_REF)
        String featureRef();
    }

    /**
     * Options of CXF clients or service endpoints.
     */
    @ConfigGroup
    interface ClientsOrEndpointsConfig {

        /**
         * WS-RM related client or service endpoint configuration
         *
         * @asciidoclet
         */
        ClientOrEndpointConfig rm();

        /**
         * Options of a CXF client or service endpoint.
         */
        @ConfigGroup
        public interface ClientOrEndpointConfig {

            /**
             * If `true` then the WS-ReliableMessaging link:https://cxf.apache.org/docs/ws-reliablemessaging.html[interceptors]
             * will be added to this client or service endpoint.
             *
             * @asciidoclet
             * @since 2.7.0
             */
            @WithDefault("true")
            boolean enabled();
        }
    }
}
