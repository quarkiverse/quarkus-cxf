package io.quarkiverse.cxf.metrics;

import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.cxf.feature.Feature;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;

import io.quarkiverse.cxf.CXFClientInfo;
import io.quarkiverse.cxf.CXFServletInfo;
import io.quarkiverse.cxf.CxfClientProducer.ClientFactoryCustomizer;
import io.quarkiverse.cxf.metrics.CxfMetricsConfig.ClientsConfig;
import io.quarkiverse.cxf.metrics.CxfMetricsConfig.EndpointsConfig;
import io.quarkiverse.cxf.transport.CxfHandler.EndpointFactoryCustomizer;

@ApplicationScoped
public class MetricsCustomizer implements ClientFactoryCustomizer, EndpointFactoryCustomizer {
    public static final String INSTRUMENTATION_SCOPE = "io.quarkiverse.cxf";

    @Inject
    CxfMetricsConfig config;

    private QuarkusCxfMetricsFeature feature;

    @PostConstruct
    void init() {
        this.feature = new QuarkusCxfMetricsFeature();
    }

    @Override
    public void customize(CXFClientInfo cxfClientInfo, JaxWsProxyFactoryBean factory) {
        if (config.metrics().enabledFor().enabledForClients()) {
            final String key = cxfClientInfo.getConfigKey();
            final Map<String, ClientsConfig> clients = config.clients();
            if (key == null
                    || clients == null
                    || !clients.containsKey(key)
                    || clients.get(key).metrics().enabled()) {
                addFeatureIfNeeded(factory.getFeatures());
            }
        }
    }

    @Override
    public void customize(CXFServletInfo servletInfo, JaxWsServerFactoryBean factory) {
        if (config.metrics().enabledFor().enabledForServices()) {
            final String key = servletInfo.getRelativePath();
            final Map<String, EndpointsConfig> endpoints = config.endpoints();
            if (key != null
                    || endpoints == null
                    || !endpoints.containsKey(key)
                    || endpoints.get(key).metrics().enabled()) {
                addFeatureIfNeeded(factory.getFeatures());
            }
        }
    }

    private void addFeatureIfNeeded(List<Feature> features) {
        if (features.stream().noneMatch(f -> f instanceof QuarkusCxfMetricsFeature)) {
            /*
             * Before 2.7.0, we recommended adding QuarkusCxfMetricsFeature manually
             * So let's add it only if it is not there already
             */
            features.add(feature);
        }
    }
}
