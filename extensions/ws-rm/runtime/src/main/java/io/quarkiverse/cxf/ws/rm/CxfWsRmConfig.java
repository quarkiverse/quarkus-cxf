package io.quarkiverse.cxf.ws.rm;

import java.util.Map;
import java.util.Optional;

import org.apache.cxf.ws.addressing.VersionTransformer.Names200408;
import org.apache.cxf.ws.rm.RM10Constants;
import org.apache.cxf.ws.rm.RetransmissionQueue;
import org.apache.cxf.ws.rm.persistence.RMStore;

import io.quarkus.runtime.annotations.ConfigDocFilename;
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
     */
    GlobalRmConfig rm();

    /**
     * Client configurations.
     */
    @WithName("client")
    Map<String, ClientsOrEndpointsConfig> clients();

    /**
     * Endpoint configurations.
     */
    @WithName("endpoint")
    Map<String, ClientsOrEndpointsConfig> endpoints();

    @ConfigGroup
    public interface GlobalRmConfig {

        /**
         * WS-RM version namespace: {@code http://schemas.xmlsoap.org/ws/2005/02/rm/} or
         * {@code http://docs.oasis-open.org/ws-rx/wsrm/200702}
         *
         * @since 2.7.0
         */
        @WithDefault(RM10Constants.NAMESPACE_URI)
        String namespace();

        /**
         * WS-Addressing version namespace: {@code http://schemas.xmlsoap.org/ws/2004/08/addressing} or
         * {@code http://www.w3.org/2005/08/addressing}. Note that this property is ignored unless you are using the
         * {@code http://schemas.xmlsoap.org/ws/2005/02/rm/} RM namespace.
         *
         * @since 2.7.0
         */
        @WithDefault(Names200408.WSA_NAMESPACE_NAME)
        String wsaNamespace();

        /**
         * A time duration in milliseconds after which the associated sequence will be closed if no messages (including
         * acknowledgments and other control messages) were exchanged between the sender and receiver during that
         * period of time. If not set, the associated sequence will never be closed due to inactivity.
         *
         * @since 2.7.0
         */
        Optional<Long> inactivityTimeout();

        /**
         * A time duration in milliseconds between successive attempts to resend a message that has not been
         * acknowledged by
         * the receiver.
         *
         * @since 2.7.0
         */
        @WithDefault(RetransmissionQueue.DEFAULT_BASE_RETRANSMISSION_INTERVAL)
        long retransmissionInterval();

        /**
         * If {@code true} the retransmission interval will be doubled on every transmission attempt; otherwise the
         * retransmission interval stays equal to {@code quarkus.cxf.rm.retransmission-interval} for every
         * retransmission attempt.
         *
         * @since 2.7.0
         */
        @WithDefault("false")
        boolean exponentialBackoff();

        /**
         * A time duration in milliseconds within which an acknowledgement for a received message is expected to be
         * sent by a RM destination. If not specified, the acknowledgements will be sent immediately.
         *
         * @since 2.7.0
         */
        Optional<Long> acknowledgementInterval();

        /**
         * A <a href="../../user-guide/configuration.html#beanRefs">reference</a> to a
         * {@code org.apache.cxf.ws.rm.persistence.RMStore} bean used to store source and destination sequences and
         * message references.
         *
         * @since 2.7.0
         */
        Optional<String> store();

        /**
         *
         *
         * @since 2.7.0
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
         */
        ClientOrEndpointConfig rm();

        /**
         * Options of a CXF client or service endpoint.
         */
        @ConfigGroup
        public interface ClientOrEndpointConfig {
            /**
             * If {@code true} then the WS-ReliableMessaging
             * <a href="https://cxf.apache.org/docs/ws-reliablemessaging.html">interceptors</a> will be added to this
             * client or service endpoint.
             */
            @WithDefault("true")
            boolean enabled();
        }
    }

}
