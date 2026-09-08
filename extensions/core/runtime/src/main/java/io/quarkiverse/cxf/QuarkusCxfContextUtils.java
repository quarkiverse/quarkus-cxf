package io.quarkiverse.cxf;

import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.ws.addressing.ContextUtils;

/**
 * Quarkus CXF overrides of {@link ContextUtils}.
 */
public class QuarkusCxfContextUtils {

    /**
     * See {@link ContextUtils#logDisallowedDecoupledDestinationScheme(Logger, Level, String)}
     *
     * @param logger the {@link Logger} to pass the message to
     * @param level log {@link Level} on which the message should be logged
     * @param destinationUri the URI banned due to the configuration
     */
    public static void logDisallowedDecoupledDestinationScheme(java.util.logging.Logger logger, Level level,
            String destinationUri) {
        logger.log(level,
                "Rejected pre-approved decoupled destination with disallowed scheme: {0}. Configure permitted URI schemes using quarkus.cxf.endpoint.addressing.decoupled.allowed-schemes configuration parameter",
                destinationUri);
    }

    /**
     * See {@link ContextUtils#logRejectedDecoupledDestination(Logger, Level, String)}
     *
     * @param logger the {@link Logger} to pass the message to
     * @param level log {@link Level} on which the message should be logged
     * @param destinationUri the URI banned due to the configuration
     */
    public static void logRejectedDecoupledDestination(Logger logger, Level level, String destinationUri) {
        logger.log(level,
                "Rejected wsa:ReplyTo/FaultTo decoupled destination: {0}. Decoupled WS-Addressing is disabled by default; enable with quarkus.cxf.endpoint.addressing.decoupled.enabled=true, or configure permitted URI schemes using quarkus.cxf.endpoint.addressing.decoupled.allowed-schemes configuration parameter",
                destinationUri);
    }

    /**
     * See {@link ContextUtils#logDecoupledFaultToSchemeNotAllowed(Logger, Level, String)}
     *
     * @param logger the {@link Logger} to pass the message to
     * @param level log {@link Level} on which the message should be logged
     * @param destinationUri the URI banned due to the configuration
     */
    public static void logDecoupledFaultToSchemeNotAllowed(Logger logger, Level level, String destinationUri) {
        logger.log(level,
                "Decoupled pre-approved FaultTo ({0}) is not permitted: URI scheme is not allowed. Fault will be delivered to ReplyTo instead. Configure permitted schemes using quarkus.cxf.endpoint.addressing.decoupled.allowed-schemes configuration parameter",
                destinationUri);
    }

    /**
     * See {@link ContextUtils#logDecoupledFaultToNotAllowed(Logger, Level, String)}
     *
     * @param logger the {@link Logger} to pass the message to
     * @param level log {@link Level} on which the message should be logged
     * @param destinationUri the URI banned due to the configuration
     */
    public static void logDecoupledFaultToNotAllowed(Logger logger, Level level, String destinationUri) {
        logger.log(level,
                "Decoupled WS-Addressing FaultTo ({0}) is not permitted; fault will be delivered to ReplyTo instead. Enable with quarkus.cxf.endpoint.addressing.decoupled.enabled=true, or configure permitted URI schemes using quarkus.cxf.endpoint.addressing.decoupled.allowed-schemes configuration parameter",
                destinationUri);
    }

    /**
     * See {@link ContextUtils#formatDecoupledReplyToNotPermittedMessage(String)}
     *
     * @param destinationUri the URI banned due to the configuration
     * @return a formatted message
     */
    public static String formatDecoupledReplyToNotPermittedMessage(String destinationUri) {
        return MessageFormat.format(
                "Decoupled WS-Addressing ReplyTo ({0}) is not permitted by this server. Enable with quarkus.cxf.endpoint.addressing.decoupled.enabled=true, or configure permitted URI schemes using quarkus.cxf.endpoint.addressing.decoupled.allowed-schemes configuration parameter",
                destinationUri);
    }

    /**
     * See {@link ContextUtils#formatDecoupledReplyToSchemeNotPermittedMessage(String)}
     *
     * @param destinationUri the URI banned due to the configuration
     * @return a formatted message
     */
    public static String formatDecoupledReplyToSchemeNotPermittedMessage(String destinationUri) {
        return MessageFormat.format(
                "Decoupled WS-Addressing ReplyTo ({0}) is not permitted by this server: URI scheme is not allowed. Configure permitted schemes using quarkus.cxf.endpoint.addressing.decoupled.allowed-schemes configuration parameter",
                destinationUri);
    }
}
