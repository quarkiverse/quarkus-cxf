package io.quarkiverse.cxf.opentelemetry;

import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.tracing.opentelemetry.OpenTelemetryClientFeature;
import org.apache.cxf.tracing.opentelemetry.OpenTelemetryFeature;

import io.opentelemetry.api.OpenTelemetry;
import io.quarkiverse.cxf.CXFClientInfo;
import io.quarkiverse.cxf.CXFServletInfo;
import io.quarkiverse.cxf.CxfClientProducer.ClientFactoryCustomizer;
import io.quarkiverse.cxf.opentelemetry.CxfOpenTelemetryConfig.ClientsConfig;
import io.quarkiverse.cxf.opentelemetry.CxfOpenTelemetryConfig.EndpointsConfig;
import io.quarkiverse.cxf.transport.CxfHandler.EndpointFactoryCustomizer;

@ApplicationScoped
public class OpenTelemetryCustomizer implements ClientFactoryCustomizer, EndpointFactoryCustomizer {
    public static final String INSTRUMENTATION_SCOPE = "io.quarkiverse.cxf";

    @Inject
    CxfOpenTelemetryConfig config;

    @Inject
    OpenTelemetry openTelemetry;

    private OpenTelemetryFeature serviceFeature;
    private OpenTelemetryClientFeature clientFeature;

    @PostConstruct
    void init() {
        this.serviceFeature = new OpenTelemetryFeature(openTelemetry, INSTRUMENTATION_SCOPE);
        this.clientFeature = new OpenTelemetryClientFeature(openTelemetry, INSTRUMENTATION_SCOPE);
    }

    @Override
    public void customize(CXFClientInfo cxfClientInfo, JaxWsProxyFactoryBean factory) {
        if (config.otel().enabledFor().enabledForClients()) {
            final String key = cxfClientInfo.getConfigKey();
            final Map<String, ClientsConfig> clients = config.clients();
            if (key == null
                    || clients == null
                    || !clients.containsKey(key)
                    || clients.get(key).otel().enabled()) {
                factory.getFeatures().add(clientFeature);
            }
        }
    }

    @Override
    public void customize(CXFServletInfo servletInfo, JaxWsServerFactoryBean factory) {
        if (config.otel().enabledFor().enabledForServices()) {
            final String key = servletInfo.getRelativePath();
            final Map<String, EndpointsConfig> endpoints = config.endpoints();
            if (key == null
                    || endpoints == null
                    || !endpoints.containsKey(key)
                    || endpoints.get(key).otel().enabled()) {
                factory.getFeatures().add(serviceFeature);
            }
        }
    }

}
