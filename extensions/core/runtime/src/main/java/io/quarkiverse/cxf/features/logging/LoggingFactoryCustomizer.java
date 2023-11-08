package io.quarkiverse.cxf.features.logging;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.jboss.logging.Logger;

import io.quarkiverse.cxf.CXFClientInfo;
import io.quarkiverse.cxf.CXFServletInfo;
import io.quarkiverse.cxf.CxfClientProducer.ClientFactoryCustomizer;
import io.quarkiverse.cxf.features.logging.CxfLoggingConfig.LoggingConfig;
import io.quarkiverse.cxf.transport.CxfHandler.EndpointFactoryCustomizer;

@ApplicationScoped
public class LoggingFactoryCustomizer implements ClientFactoryCustomizer, EndpointFactoryCustomizer {
    private static final Logger log = Logger.getLogger(LoggingFactoryCustomizer.class);
    @Inject
    CxfLoggingConfig loggingConfig;

    @Override
    public void customize(CXFClientInfo cxfClientInfo, JaxWsProxyFactoryBean factory) {
        final String key = cxfClientInfo.getConfigKey();
        if (key != null && Optional.ofNullable(loggingConfig.clients()).map(m -> m.containsKey(key)).orElse(false)) {
            final LoggingConfig wssConfig = Optional.ofNullable(loggingConfig.clients()).map(m1 -> m1.get(key)).orElse(null)
                    .logging();
            final List<Feature> features = factory.getFeatures();
            customize(Kind.client, key, wssConfig, features);
        }
    }

    @Override
    public void customize(CXFServletInfo cxfServletInfo, JaxWsServerFactoryBean factory) {
        final String key = cxfServletInfo.getRelativePath();
        if (key != null && Optional.ofNullable(loggingConfig.endpoints()).map(m -> m.containsKey(key)).orElse(false)) {
            final LoggingConfig wssConfig = Optional.ofNullable(loggingConfig.endpoints()).map(m1 -> m1.get(key)).orElse(null)
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
            LoggingConfig config,
            List<Feature> features) {

        if (!config.enabled()) {
            log.debugf("Logging feature disabled for %s \"%s\"", kind, key);
            return;
        }
        if (features.stream().anyMatch(i -> i instanceof LoggingFeature)) {
            throw new IllegalStateException(LoggingFeature.class.getSimpleName() + " already configured for " + kind + " \""
                    + key + "\". Either remove all quarkus.cxf." + kind + ".\"" + key + "\".logging.* options or the "
                    + LoggingFeature.class.getSimpleName() + " you added programmatically or via quarkus.cxf." + kind + ".\""
                    + key + "\".features");
        }
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

        features.add(feature);
    }

    private void addList(Optional<List<String>> source, Consumer<String> consumer) {
        source.ifPresent(list -> consumer.accept(list.stream().collect(Collectors.joining(";"))));
    }

}
