package io.quarkiverse.cxf.ws.rm;

import java.util.Map;
import java.util.function.BiConsumer;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.cxf.BusFactory;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.ws.rm.RM10Constants;
import org.apache.cxf.ws.rm.RMManager;
import org.apache.cxf.ws.rm.feature.RMFeature;

import io.quarkiverse.cxf.CXFClientInfo;
import io.quarkiverse.cxf.CXFRuntimeUtils;
import io.quarkiverse.cxf.CXFServletInfo;
import io.quarkiverse.cxf.CxfClientProducer.ClientFactoryCustomizer;
import io.quarkiverse.cxf.transport.CxfHandler.EndpointFactoryCustomizer;
import io.quarkiverse.cxf.ws.rm.CxfWsRmConfig.ClientsOrEndpointsConfig;
import io.quarkiverse.cxf.ws.rm.CxfWsRmConfig.GlobalRmConfig;

@ApplicationScoped
public class WsRmFactoryCustomizer implements ClientFactoryCustomizer, EndpointFactoryCustomizer {
    @Inject
    CxfWsRmConfig config;

    private RMFeature rmFeature;

    @PostConstruct
    void init() {
        this.rmFeature = CXFRuntimeUtils.getInstance(config.rm().featureRef(), true);
    }

    @Override
    public void customize(CXFClientInfo cxfClientInfo, JaxWsProxyFactoryBean factory) {
        final String key = cxfClientInfo.getConfigKey();
        final Map<String, ClientsOrEndpointsConfig> clients = config.clients();
        if (key == null
                || clients == null
                || !clients.containsKey(key)
                || clients.get(key).rm().enabled()) {
            customize(factory, factory.getProperties()::put);
        }
    }

    @Override
    public void customize(CXFServletInfo servletInfo, JaxWsServerFactoryBean factory) {
        final String key = servletInfo.getRelativePath();
        final Map<String, ClientsOrEndpointsConfig> endpoints = config.endpoints();
        if (key == null
                || endpoints == null
                || !endpoints.containsKey(key)
                || endpoints.get(key).rm().enabled()) {
            customize(factory, factory.getProperties()::put);
        }
    }

    private void customize(
            InterceptorProvider interceptorProvider,
            BiConsumer<String, Object> props) {
        final GlobalRmConfig globalRmConfig = config.rm();

        if (globalRmConfig.featureRef().equals(DefaultRmFeatureProducer.DEFAULT_RM_FEATURE_REF)) {
            String ns = globalRmConfig.namespace();
            props.accept(RMManager.WSRM_VERSION_PROPERTY, ns);
            if (RM10Constants.NAMESPACE_URI.equals(ns)) {
                props.accept(RMManager.WSRM_WSA_VERSION_PROPERTY, globalRmConfig.wsaNamespace());
            }
            globalRmConfig.inactivityTimeout()
                    .ifPresent(val -> props.accept(RMManager.WSRM_INACTIVITY_TIMEOUT_PROPERTY, val));
            props.accept(RMManager.WSRM_RETRANSMISSION_INTERVAL_PROPERTY, globalRmConfig.retransmissionInterval());
            props.accept(RMManager.WSRM_EXPONENTIAL_BACKOFF_PROPERTY, globalRmConfig.exponentialBackoff());
            globalRmConfig.acknowledgementInterval()
                    .ifPresent(val -> props.accept(RMManager.WSRM_ACKNOWLEDGEMENT_INTERVAL_PROPERTY, val));

        }

        /* Install WS-RM interceptors on this client */
        this.rmFeature.doInitializeProvider(interceptorProvider, BusFactory.getDefaultBus());
    }

}
