package io.quarkiverse.cxf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.cxf.Bus;
import org.apache.cxf.transport.http.HTTPConduitFactory;
import org.jboss.logging.Logger;

import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue.Supplier;
import io.quarkiverse.cxf.annotation.CXFEndpoint;
import io.quarkiverse.cxf.transport.CxfHandler;
import io.quarkiverse.cxf.transport.VertxDestinationFactory;
import io.quarkiverse.cxf.vertx.http.client.VertxHttpClientHTTPConduitFactory;
import io.quarkus.arc.Arc;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.HttpConfiguration;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class CXFRecorder {
    private static final Logger LOGGER = Logger.getLogger(CXFRecorder.class);
    private static boolean hc5Present = false;

    /**
     * Stores the given {@link CXFClientData} in the application.
     */
    public RuntimeValue<CXFClientData> cxfClientData(CXFClientData cxfClientData) {
        return new RuntimeValue<>(cxfClientData);
    }

    public static class ServletConfig {
        public CxfEndpointConfig config;
        public String path;

        public ServletConfig(CxfEndpointConfig cxfEndPointConfig, String relativePath) {
            this.config = cxfEndPointConfig;
            this.path = relativePath;
        }
    }

    public enum BeanLookupStrategy {
        TYPE {
            @Override
            public Supplier<Object> createLookUp(Class<?> sei, Class<?> wsImplementor, String path) {
                return () -> CXFRuntimeUtils.getInstance(wsImplementor);
            }
        },
        METHOD_WITH_CXFENDPOINT_ANNOTATION {
            @Override
            public Supplier<Object> createLookUp(Class<?> sei, Class<?> wsImplementor, String path) {
                return createLookupInternal(sei, path);
            }

        },
        TYPE_WITH_CXFENDPOINT_ANNOTATION {
            @Override
            public Supplier<Object> createLookUp(Class<?> sei, Class<?> wsImplementor, String path) {
                return createLookupInternal(wsImplementor, path);
            }

        };

        private static Supplier<Object> createLookupInternal(Class<?> type, String path) {
            return () -> {
                Object result = Arc.container()
                        .instance(type,
                                new CXFEndpoint.CXFEndpointLiteral(path))
                        .get();
                if (result == null) {
                    throw new IllegalStateException("Could get bean of type " + type + " qualified by @"
                            + CXFEndpoint.class.getName() + "(\"" + path + "\")");
                }
                return result;
            };
        }

        public abstract Supplier<Object> createLookUp(Class<?> sei, Class<?> wsImplementor, String path);
    }

    public void addCxfServletInfo(
            RuntimeValue<CXFServletInfos> runtimeInfos,
            RuntimeValue<Map<String, List<ServletConfig>>> implementorToCfg,
            String path,
            Class<?> sei,
            CxfConfig cxfConfig,
            String serviceName,
            String serviceTargetNamepsace,
            String soapBinding,
            Class<?> wsImplementor,
            Boolean isProvider,
            String relativePathFromCxfEndpointAnnotation,
            BeanLookupStrategy beanLookupStrategy) {

        CXFServletInfos infos = runtimeInfos.getValue();

        switch (beanLookupStrategy) {
            case METHOD_WITH_CXFENDPOINT_ANNOTATION:
            case TYPE_WITH_CXFENDPOINT_ANNOTATION: {
                final CxfEndpointConfig cxfEndPointConfig = cxfConfig.endpoints().get(relativePathFromCxfEndpointAnnotation);
                final CXFServletInfo info = createServletInfo(
                        path,
                        sei,
                        serviceName,
                        serviceTargetNamepsace,
                        soapBinding,
                        wsImplementor,
                        cxfEndPointConfig,
                        relativePathFromCxfEndpointAnnotation,
                        isProvider,
                        beanLookupStrategy.createLookUp(sei, wsImplementor, relativePathFromCxfEndpointAnnotation));
                infos.add(info);
                return;
            }
            case TYPE: {
                if (relativePathFromCxfEndpointAnnotation != null) {
                    final CxfEndpointConfig cxfEndPointConfig = cxfConfig.endpoints()
                            .get(relativePathFromCxfEndpointAnnotation);
                    final CXFServletInfo info = createServletInfo(
                            path,
                            sei,
                            serviceName,
                            serviceTargetNamepsace,
                            soapBinding,
                            wsImplementor,
                            cxfEndPointConfig,
                            relativePathFromCxfEndpointAnnotation,
                            isProvider,
                            beanLookupStrategy.createLookUp(sei, wsImplementor, relativePathFromCxfEndpointAnnotation));
                    infos.add(info);
                    return;
                }
                List<ServletConfig> cfgs = implementorToCfg.getValue().get(wsImplementor.getName());
                if (cfgs != null) {
                    for (ServletConfig cfg : cfgs) {
                        CxfEndpointConfig cxfEndPointConfig = cfg.config;
                        String relativePath = cfg.path;
                        final CXFServletInfo info = createServletInfo(
                                path,
                                sei,
                                serviceName,
                                serviceTargetNamepsace,
                                soapBinding,
                                wsImplementor,
                                cxfEndPointConfig,
                                relativePath,
                                isProvider,
                                beanLookupStrategy.createLookUp(sei, wsImplementor, relativePathFromCxfEndpointAnnotation));
                        infos.add(info);
                    }
                } else {
                    if (serviceName == null || serviceName.isEmpty()) {
                        serviceName = sei.getName().toLowerCase();
                        if (serviceName.contains(".")) {
                            serviceName = serviceName.substring(serviceName.lastIndexOf('.') + 1);
                        }
                    }
                    final String relativePath = "/" + serviceName;
                    final CXFServletInfo info = createServletInfo(
                            path,
                            sei,
                            serviceName,
                            serviceTargetNamepsace,
                            soapBinding,
                            wsImplementor,
                            null,
                            relativePath,
                            isProvider,
                            beanLookupStrategy.createLookUp(sei, wsImplementor, relativePathFromCxfEndpointAnnotation));
                    infos.add(info);
                }
                return;
            }
            default:
                throw new IllegalArgumentException("Unexpected BeanLookupStrategy " + beanLookupStrategy);
        }
    }

    public RuntimeValue<Map<String, List<ServletConfig>>> implementorToCfgMap(CxfConfig cxfConfig) {
        Map<String, List<ServletConfig>> implementorToCfg = new HashMap<>();
        for (Map.Entry<String, CxfEndpointConfig> webServicesByPath : cxfConfig.endpoints().entrySet()) {
            CxfEndpointConfig cxfEndPointConfig = webServicesByPath.getValue();
            String relativePath = webServicesByPath.getKey();
            if (!cxfEndPointConfig.implementor().isPresent()) {
                continue;
            }
            String cfgImplementor = cxfEndPointConfig.implementor().get();
            List<ServletConfig> lst;
            if (implementorToCfg.containsKey(cfgImplementor)) {
                lst = implementorToCfg.get(cfgImplementor);
            } else {
                lst = new ArrayList<>();
                implementorToCfg.put(cfgImplementor, lst);
            }
            lst.add(new ServletConfig(cxfEndPointConfig, relativePath));
        }
        return new RuntimeValue<>(implementorToCfg);
    }

    private static CXFServletInfo createServletInfo(
            String path,
            Class<?> sei,
            String serviceName,
            String serviceTargetNamespace,
            String soapBinding,
            Class<?> implementor,
            CxfEndpointConfig cxfEndPointConfig,
            String relativePath,
            Boolean isProvider,
            Supplier<Object> beanLookup) {
        CXFServletInfo cfg = new CXFServletInfo(
                path,
                relativePath,
                implementor,
                sei,
                cxfEndPointConfig != null ? cxfEndPointConfig.wsdlPath().orElse(null) : null,
                serviceName,
                serviceTargetNamespace,
                cxfEndPointConfig != null ? cxfEndPointConfig.soapBinding().orElse(soapBinding) : soapBinding,
                isProvider,
                cxfEndPointConfig != null ? cxfEndPointConfig.publishedEndpointUrl().orElse(null) : null,
                cxfEndPointConfig != null ? cxfEndPointConfig.schemaValidationEnabledFor().orElse(null) : null,
                beanLookup);
        if (cxfEndPointConfig != null && cxfEndPointConfig.inInterceptors().isPresent()) {
            cfg.addInInterceptors(cxfEndPointConfig.inInterceptors().get());
        }
        if (cxfEndPointConfig != null && cxfEndPointConfig.outInterceptors().isPresent()) {
            cfg.addOutInterceptors(cxfEndPointConfig.outInterceptors().get());
        }
        if (cxfEndPointConfig != null && cxfEndPointConfig.outFaultInterceptors().isPresent()) {
            cfg.addOutFaultInterceptors(cxfEndPointConfig.outFaultInterceptors().get());
        }
        if (cxfEndPointConfig != null && cxfEndPointConfig.inFaultInterceptors().isPresent()) {
            cfg.addInFaultInterceptors(cxfEndPointConfig.inFaultInterceptors().get());
        }
        if (cxfEndPointConfig != null && cxfEndPointConfig.features().isPresent()) {
            cfg.addFeatures(cxfEndPointConfig.features().get());
        }
        if (cxfEndPointConfig != null && cxfEndPointConfig.handlers().isPresent()) {
            cfg.addHandlers(cxfEndPointConfig.handlers().get());
        }
        LOGGER.tracef("Registering CXF Servlet info %s", cfg);
        return cfg;
    }

    public RuntimeValue<CXFServletInfos> createInfos(String path, String contextPath) {
        CXFServletInfos infos = new CXFServletInfos(path, contextPath);
        return new RuntimeValue<>(infos);
    }

    public Handler<RoutingContext> initServer(
            RuntimeValue<CXFServletInfos> infos,
            BeanContainer beanContainer,
            HttpConfiguration httpConfiguration,
            CxfFixedConfig fixedConfig) {
        LOGGER.trace("init server");
        return new CxfHandler(infos.getValue(), beanContainer, httpConfiguration, fixedConfig);
    }

    public void resetDestinationRegistry(ShutdownContext context) {
        context.addShutdownTask(VertxDestinationFactory::resetRegistry);
    }

    public void addRuntimeBusCustomizer(RuntimeValue<Consumer<Bus>> customizer) {
        QuarkusBusFactory.addBusCustomizer(customizer.getValue());
    }

    public RuntimeValue<Consumer<Bus>> setVertxHttpClientHTTPConduitFactory() {
        return new RuntimeValue<>(bus -> bus.setExtension(new VertxHttpClientHTTPConduitFactory(), HTTPConduitFactory.class));
    }

    public RuntimeValue<Consumer<Bus>> setURLConnectionHTTPConduitFactory() {
        return new RuntimeValue<>(bus -> bus.setExtension(new URLConnectionHTTPConduitFactory(), HTTPConduitFactory.class));
    }

    public RuntimeValue<Consumer<Bus>> setHttpClientHTTPConduitFactory() {
        return new RuntimeValue<>(bus -> bus.setExtension(new HttpClientHTTPConduitFactory(), HTTPConduitFactory.class));
    }

    public void setHc5Present() {
        hc5Present = true;
    }

    public static boolean isHc5Present() {
        return hc5Present;
    }

}
