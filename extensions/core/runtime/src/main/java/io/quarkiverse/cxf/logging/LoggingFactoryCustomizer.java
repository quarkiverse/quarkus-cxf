package io.quarkiverse.cxf.logging;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.jboss.logging.Logger;

import io.quarkiverse.cxf.CXFClientInfo;
import io.quarkiverse.cxf.CXFServletInfo;
import io.quarkiverse.cxf.CxfConfig;
import io.quarkiverse.cxf.LoggingConfig;
import io.quarkiverse.cxf.LoggingConfig.GlobalLoggingConfig;
import io.quarkiverse.cxf.LoggingConfig.PerClientOrServiceLoggingConfig;

public class LoggingFactoryCustomizer {
    private static final Logger log = Logger.getLogger(LoggingFactoryCustomizer.class);
    private final CxfConfig config;
    private final LoggingFeature globalLoggingFeature;

    public LoggingFactoryCustomizer(CxfConfig config) {
        super();
        this.config = config;

        final GlobalLoggingConfig globalLoggingConfig = config.logging();
        if (globalLoggingConfig.enabledFor().enabledForAny()) {
            this.globalLoggingFeature = configureLoggingFeature(globalLoggingConfig);
        } else {
            log.debugf("Global logging feature is disabled");
            this.globalLoggingFeature = null;
        }

    }

    public void customize(CXFClientInfo cxfClientInfo, JaxWsProxyFactoryBean factory) {
        final String key = cxfClientInfo.getConfigKey();
        if (key != null && Optional.ofNullable(config.clients()).map(m -> m.containsKey(key)).orElse(false)) {
            final PerClientOrServiceLoggingConfig wssConfig = Optional.ofNullable(config.clients()).map(m1 -> m1.get(key))
                    .orElse(null)
                    .logging();
            final List<Feature> features = factory.getFeatures();
            customize(Kind.client, key, wssConfig, features);
        }
    }

    public void customize(CXFServletInfo cxfServletInfo, JaxWsServerFactoryBean factory) {
        final String key = cxfServletInfo.getRelativePath();
        if (key != null && Optional.ofNullable(config.endpoints()).map(m -> m.containsKey(key)).orElse(false)) {
            final PerClientOrServiceLoggingConfig wssConfig = Optional.ofNullable(config.endpoints()).map(m1 -> m1.get(key))
                    .orElse(null)
                    .logging();
            final List<Feature> features = factory.getFeatures();
            customize(Kind.endpoint, key, wssConfig, features);
        }
    }

    private enum Kind {
        client,
        endpoint
    };

    private void customize(
            Kind kind,
            final String key,
            PerClientOrServiceLoggingConfig wssConfig,
            List<Feature> features) {

        final LoggingFeature loggingFeature;

        final Optional<Boolean> enabled = wssConfig.enabled();
        if (enabled.isPresent()) {
            loggingFeature = enabled.get().booleanValue()
                    ? configureLoggingFeature(wssConfig)
                    /*
                     * if the user sets .enabled = false on the client/service level explicitly,
                     * then he wants to mute the global setting
                     */
                    : null;
        } else if (config.logging().enabledFor().enabledForClients()) {
            loggingFeature = this.globalLoggingFeature;
            if (loggingFeature != null) {
                log.debugf("Logging feature not enabled explicitly for %s \"%s\", using the global logging feature", kind, key);
            }
        } else {
            loggingFeature = null;
            log.debugf(
                    "Logging feature not enabled explicitly for %s \"%s\", the global logging feature is not enabled for "
                            + kind.name() + "s either",
                    kind, key);
        }

        if (loggingFeature != null) {
            if (features.stream().anyMatch(i -> i instanceof LoggingFeature)) {
                throw new IllegalStateException(LoggingFeature.class.getSimpleName() + " already configured for " + kind + " \""
                        + key + "\". Either remove all quarkus.cxf." + kind + ".\"" + key + "\".logging.* options or the "
                        + LoggingFeature.class.getSimpleName() + " you added programmatically or via quarkus.cxf." + kind
                        + ".\""
                        + key + "\".features");
            }
            features.add(loggingFeature);
        }
    }

    private LoggingFeature configureLoggingFeature(LoggingConfig config) {
        LoggingFeature feature = new LoggingFeature();
        feature.setLimit(config.limit());
        feature.setInMemThreshold(config.inMemThreshold());
        feature.setPrettyLogging(config.pretty());
        feature.setLogBinary(config.logBinary());
        feature.setLogMultipart(config.logMultipart());
        feature.setVerbose(config.verbose());

        addList(config.inBinaryContentMediaTypes(), feature::addInBinaryContentMediaTypes);
        addList(config.outBinaryContentMediaTypes(), feature::addOutBinaryContentMediaTypes);
        addList(config.binaryContentMediaTypes(), feature::addBinaryContentMediaTypes);
        config.sensitiveElementNames().ifPresent(feature::addSensitiveElementNames);
        config.sensitiveProtocolHeaderNames().ifPresent(feature::addSensitiveProtocolHeaderNames);
        return feature;
    }

    private void addList(Optional<List<String>> source, Consumer<String> consumer) {
        source.ifPresent(list -> consumer.accept(list.stream().collect(Collectors.joining(";"))));
    }

}
