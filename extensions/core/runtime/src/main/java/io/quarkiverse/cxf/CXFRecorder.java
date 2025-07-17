package io.quarkiverse.cxf;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.cxf.Bus;
import org.apache.cxf.io.CachedConstants;
import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.apache.cxf.wsdl.WSDLManager;
import org.jboss.logging.Logger;

import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue.Supplier;
import io.quarkiverse.cxf.CxfConfig.RetransmitCacheConfig;
import io.quarkiverse.cxf.annotation.CXFEndpoint;
import io.quarkiverse.cxf.transport.CxfHandler;
import io.quarkiverse.cxf.transport.VertxDestinationFactory;
import io.quarkiverse.cxf.wsdl.QuarkusWSDLManager;
import io.quarkus.arc.Arc;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.VertxHttpConfig;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class CXFRecorder {
    private static final Logger LOGGER = Logger.getLogger(CXFRecorder.class);
    private final RuntimeValue<VertxHttpConfig> httpConfiguration;
    private final RuntimeValue<CxfConfig> cxfConfig;

    public CXFRecorder(RuntimeValue<VertxHttpConfig> httpConfiguration, RuntimeValue<CxfConfig> cxfConfig) {
        super();
        this.httpConfiguration = httpConfiguration;
        this.cxfConfig = cxfConfig;
    }

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
                final CxfEndpointConfig cxfEndPointConfig = cxfConfig.getValue().endpoints()
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
            case TYPE: {
                if (relativePathFromCxfEndpointAnnotation != null) {
                    final CxfEndpointConfig cxfEndPointConfig = cxfConfig.getValue().endpoints()
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

    public RuntimeValue<Map<String, List<ServletConfig>>> implementorToCfgMap() {
        Map<String, List<ServletConfig>> implementorToCfg = new HashMap<>();
        for (Map.Entry<String, CxfEndpointConfig> webServicesByPath : cxfConfig.getValue().endpoints().entrySet()) {
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
            CxfFixedConfig fixedConfig) {
        LOGGER.trace("init server");
        return new CxfHandler(infos.getValue(), beanContainer, httpConfiguration.getValue(), fixedConfig);
    }

    public void resetDestinationRegistry(ShutdownContext context) {
        context.addShutdownTask(VertxDestinationFactory::resetRegistry);
    }

    public void addRuntimeBusCustomizer(RuntimeValue<Consumer<Bus>> customizer) {
        QuarkusBusFactory.addBusCustomizer(customizer.getValue());
    }

    public RuntimeValue<Consumer<Bus>> setBusHTTPConduitFactory(HTTPConduitImpl factory) {
        return new RuntimeValue<>(bus -> bus.setExtension(factory, HTTPConduitSpec.class));
    }

    /**
     * Temporary workaround for https://github.com/quarkiverse/quarkus-cxf/issues/1608
     */
    public RuntimeValue<Consumer<Bus>> setQuarkusWSDLManager() {
        return new RuntimeValue<>(bus -> bus.setExtension(QuarkusWSDLManager.newInstance(bus), WSDLManager.class));
    }

    public void workaroundBadForceURLConnectionInit() {
        // A workaround for the bad initialization of HTTPTransportFactory.forceURLConnectionConduit
        // in the downstream CXF 4.0.5.fuse-redhat-00012:
        // private static boolean forceURLConnectionConduit
        // = Boolean.valueOf(SystemPropertyAction.getProperty("org.apache.cxf.transport.http.forceURLConnection", "true"));
        // Using default "true" breaks the backwards compatibility for us, so we set the property to false at application startup
        // See also https://issues.redhat.com/browse/CEQ-10395
        Field forceURLConnectionConduitField = null;
        for (Field f : HTTPTransportFactory.class.getDeclaredFields()) {
            if (f.getName().equals("forceURLConnectionConduit")) {
                f.setAccessible(true);
                forceURLConnectionConduitField = f;
                break;
            }
        }

        if (forceURLConnectionConduitField != null && Modifier.isStatic(forceURLConnectionConduitField.getModifiers())) {
            /* We are on a CXF version having the static HTTPTransportFactory.forceURLConnectionConduit field */
            try {
                /*
                 * If the property is null, but HTTPTransportFactory.forceURLConnectionConduit is true
                 * then we are on a CXF version using the wrong default and we switch to default false
                 */
                if (forceURLConnectionConduitField.getBoolean(null)
                        && System.getProperty("org.apache.cxf.transport.http.forceURLConnection") == null) {
                    System.setProperty("org.apache.cxf.transport.http.forceURLConnection", "false");
                    forceURLConnectionConduitField.set(null, false);
                }
            } catch (IllegalArgumentException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public RuntimeValue<Consumer<Bus>> busConfigForRetransmitCache() {
        return new RuntimeValue<>(bus -> {
            final RetransmitCacheConfig config = cxfConfig.getValue().retransmitCache();
            bus.setProperty(CachedConstants.THRESHOLD_BUS_PROP, String.valueOf(config.threshold().asLongValue()));
            config.maxSize().ifPresent(
                    maxSize -> bus.setProperty(CachedConstants.MAX_SIZE_BUS_PROP, String.valueOf(maxSize.asLongValue())));
            config.directory().ifPresent(dir -> bus.setProperty(CachedConstants.OUTPUT_DIRECTORY_BUS_PROP, dir));
            bus.setProperty(CachedConstants.CLEANER_DELAY_BUS_PROP, config.gcDelay().toMillis());
            bus.setProperty(CachedConstants.CLEANER_CLEAN_ON_SHUTDOWN_BUS_PROP, config.gcOnShutDown());
        });
    }

}
