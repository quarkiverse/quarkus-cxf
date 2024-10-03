package io.quarkiverse.cxf.logging;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;

import io.quarkiverse.cxf.CXFClientInfo;
import io.quarkiverse.cxf.CXFServletInfo;
import io.quarkiverse.cxf.CxfConfig;
import io.quarkiverse.cxf.EnabledFor;
import io.quarkiverse.cxf.LoggingConfig.GlobalLoggingConfig;
import io.quarkiverse.cxf.LoggingConfig.PerClientOrServiceLoggingConfig;
import io.quarkiverse.cxf.PrettyBoolean;

public class LoggingFactoryCustomizer {
    private final CxfConfig config;

    public LoggingFactoryCustomizer(CxfConfig config) {
        super();
        this.config = config;
    }

    public void customize(CXFClientInfo cxfClientInfo, JaxWsProxyFactoryBean factory) {
        final String key = cxfClientInfo.getConfigKey();
        final PerClientOrServiceLoggingConfig clientOrServiceConfig = config.clients().get(key).logging();
        customize(Kind.client, key, clientOrServiceConfig, factory.getFeatures());
    }

    public void customize(CXFServletInfo cxfServletInfo, JaxWsServerFactoryBean factory) {
        final String key = cxfServletInfo.getRelativePath();
        final PerClientOrServiceLoggingConfig clientOrServiceConfig = config.endpoints().get(key).logging();
        customize(Kind.endpoint, key, clientOrServiceConfig, factory.getFeatures());
    }

    private void customize(
            Kind kind,
            final String key,
            PerClientOrServiceLoggingConfig clientOrServiceConfig,
            List<Feature> features) {

        if (isEnabledFor(config.logging().enabledFor(), kind, clientOrServiceConfig.enabled())) {
            final LoggingFeature loggingFeature = configureLoggingFeature(config.logging(), clientOrServiceConfig);
            features.add(loggingFeature);
        }
    }

    enum Kind {
        client,
        endpoint
    };

    private LoggingFeature configureLoggingFeature(GlobalLoggingConfig global,
            PerClientOrServiceLoggingConfig clientOrServiceConfig) {
        LoggingFeature feature = new LoggingFeature();
        feature.setLimit(clientOrServiceConfig.limit().orElse(global.limit()));
        feature.setInMemThreshold(clientOrServiceConfig.inMemThreshold().orElse(global.inMemThreshold()));
        feature.setPrettyLogging(
                clientOrServiceConfig.pretty()
                        .orElse(
                                clientOrServiceConfig.enabled().map(PrettyBoolean::pretty)
                                        .orElse(global.pretty())));
        feature.setLogBinary(clientOrServiceConfig.logBinary().orElse(global.logBinary()));
        feature.setLogMultipart(clientOrServiceConfig.logMultipart().orElse(global.logMultipart()));
        feature.setVerbose(clientOrServiceConfig.verbose().orElse(global.verbose()));

        addList(global.inBinaryContentMediaTypes(), clientOrServiceConfig.inBinaryContentMediaTypes(),
                feature::addInBinaryContentMediaTypes);
        addList(global.outBinaryContentMediaTypes(), clientOrServiceConfig.outBinaryContentMediaTypes(),
                feature::addOutBinaryContentMediaTypes);
        addList(global.binaryContentMediaTypes(), clientOrServiceConfig.inBinaryContentMediaTypes(),
                feature::addBinaryContentMediaTypes);

        clientOrServiceConfig.sensitiveElementNames()
                .or(global::sensitiveElementNames)
                .ifPresent(feature::addSensitiveElementNames);
        clientOrServiceConfig.sensitiveProtocolHeaderNames()
                .or(global::sensitiveProtocolHeaderNames)
                .ifPresent(feature::addSensitiveProtocolHeaderNames);
        return feature;
    }

    private void addList(Optional<List<String>> global, Optional<List<String>> perClientOrService, Consumer<String> consumer) {
        if (perClientOrService.isPresent()) {
            consumer.accept(perClientOrService.get().stream().collect(Collectors.joining(";")));
        } else {
            global.ifPresent(list -> consumer.accept(list.stream().collect(Collectors.joining(";"))));
        }
    }

    static boolean isEnabledFor(EnabledFor global, Kind kind, Optional<PrettyBoolean> clientOrEndpoint) {
        if (clientOrEndpoint.isPresent()) {
            return clientOrEndpoint.get().enabled();
        } else {
            switch (kind) {
                case client: {
                    return global.enabledForClients();
                }
                case endpoint: {
                    return global.enabledForServices();
                }
                default:
                    throw new IllegalArgumentException("Unexpected value of " + Kind.class.getName() + ": " + kind);
            }
        }
    }

}
