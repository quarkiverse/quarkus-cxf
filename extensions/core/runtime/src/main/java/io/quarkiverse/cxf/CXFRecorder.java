package io.quarkiverse.cxf;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;

import io.quarkiverse.cxf.devconsole.DevCxfServerInfosSupplier;
import io.quarkiverse.cxf.transport.CxfHandler;
import io.quarkiverse.cxf.transport.VertxDestinationFactory;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.HttpConfiguration;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;

@Recorder
public class CXFRecorder {
    private static final Logger LOGGER = Logger.getLogger(CXFRecorder.class);
    private static final String DEFAULT_EP_ADDR = "http://localhost:8080";

    /**
     * Create CXFClientInfo supplier.
     * <p>
     * This method is called once per @WebService *interface*. The idea is to produce a default client config for a
     * given SEI.
     */
    public RuntimeValue<CXFClientInfo> cxfClientInfoSupplier(CXFClientData cxfClientData) {
        return new RuntimeValue<>(new CXFClientInfo(
                cxfClientData.getSei(),
                format("%s/%s", DEFAULT_EP_ADDR, cxfClientData.getSei().toLowerCase()),
                cxfClientData.getSoapBinding(),
                cxfClientData.getWsNamespace(),
                cxfClientData.getWsName(),
                cxfClientData.isProxyClassRuntimeInitialized()));
    }

    private static class ServletConfig {
        public CxfEndpointConfig config;
        public String path;

        public ServletConfig(CxfEndpointConfig cxfEndPointConfig, String relativePath) {
            this.config = cxfEndPointConfig;
            this.path = relativePath;
        }
    }

    public void addCxfServletInfo(RuntimeValue<CXFServletInfos> runtimeInfos, String path, String sei,
            CxfConfig cxfConfig, String serviceName, String serviceTargetNamepsace, String soapBinding,
            String wsImplementor, Boolean isProvider) {
        CXFServletInfos infos = runtimeInfos.getValue();
        Map<String, List<ServletConfig>> implementorToCfg = new HashMap<>();
        for (Map.Entry<String, CxfEndpointConfig> webServicesByPath : cxfConfig.endpoints.entrySet()) {
            CxfEndpointConfig cxfEndPointConfig = webServicesByPath.getValue();
            String relativePath = webServicesByPath.getKey();
            if (!cxfEndPointConfig.implementor.isPresent()) {
                continue;
            }
            String cfgImplementor = cxfEndPointConfig.implementor.get();
            List<ServletConfig> lst;
            if (implementorToCfg.containsKey(cfgImplementor)) {
                lst = implementorToCfg.get(cfgImplementor);
            } else {
                lst = new ArrayList<>();
                implementorToCfg.put(cfgImplementor, lst);
            }
            lst.add(new ServletConfig(cxfEndPointConfig, relativePath));
        }
        List<ServletConfig> cfgs = implementorToCfg.get(wsImplementor);
        if (cfgs != null) {
            for (ServletConfig cfg : cfgs) {
                CxfEndpointConfig cxfEndPointConfig = cfg.config;
                String relativePath = cfg.path;
                final CXFServletInfo info = createServletInfo(path, sei, serviceName, serviceTargetNamepsace, soapBinding,
                        wsImplementor,
                        cxfEndPointConfig, relativePath, isProvider);
                infos.add(info);
            }
        } else {
            if (serviceName == null || serviceName.isEmpty()) {
                serviceName = sei.toLowerCase();
                if (serviceName.contains(".")) {
                    serviceName = serviceName.substring(serviceName.lastIndexOf('.') + 1);
                }
            }
            final String relativePath = "/" + serviceName;
            final CXFServletInfo info = createServletInfo(path, sei, serviceName, serviceTargetNamepsace, soapBinding,
                    wsImplementor, null, relativePath, isProvider);
            infos.add(info);
        }
    }

    private static CXFServletInfo createServletInfo(String path, String sei, String serviceName, String serviceTargetNamespace,
            String soapBinding, String wsImplementor,
            CxfEndpointConfig cxfEndPointConfig, String relativePath, Boolean isProvider) {
        CXFServletInfo cfg = new CXFServletInfo(path,
                relativePath,
                wsImplementor,
                sei,
                cxfEndPointConfig != null ? cxfEndPointConfig.wsdlPath.orElse(null) : null,
                serviceName,
                serviceTargetNamespace,
                cxfEndPointConfig != null ? cxfEndPointConfig.soapBinding.orElse(soapBinding) : soapBinding,
                isProvider,
                cxfEndPointConfig != null ? cxfEndPointConfig.publishedEndpointUrl.orElse(null) : null);
        if (cxfEndPointConfig != null && cxfEndPointConfig.inInterceptors.isPresent()) {
            cfg.addInInterceptors(cxfEndPointConfig.inInterceptors.get());
        }
        if (cxfEndPointConfig != null && cxfEndPointConfig.outInterceptors.isPresent()) {
            cfg.addOutInterceptors(cxfEndPointConfig.outInterceptors.get());
        }
        if (cxfEndPointConfig != null && cxfEndPointConfig.outFaultInterceptors.isPresent()) {
            cfg.addOutFaultInterceptors(cxfEndPointConfig.outFaultInterceptors.get());
        }
        if (cxfEndPointConfig != null && cxfEndPointConfig.inFaultInterceptors.isPresent()) {
            cfg.addInFaultInterceptors(cxfEndPointConfig.inFaultInterceptors.get());
        }
        if (cxfEndPointConfig != null && cxfEndPointConfig.features.isPresent()) {
            cfg.addFeatures(cxfEndPointConfig.features.get());
        }
        if (cxfEndPointConfig != null && cxfEndPointConfig.handlers.isPresent()) {
            cfg.addHandlers(cxfEndPointConfig.handlers.get());
        }
        LOGGER.tracef("Registering CXF Servlet info %s", cfg);
        return cfg;
    }

    public RuntimeValue<CXFServletInfos> createInfos(String path, String contextPath) {
        CXFServletInfos infos = new CXFServletInfos(path, contextPath);
        return new RuntimeValue<>(infos);
    }

    public void initServer(
            RuntimeValue<CXFServletInfos> infosValue,
            BeanContainer beanContainer,
            HttpConfiguration httpConfiguration,
            RuntimeValue<Router> routerValue,
            ShutdownContext shutdown) {
        LOGGER.trace("init server");
        // There may be a better way to handle this
        final CXFServletInfos infos = infosValue.getValue();
        DevCxfServerInfosSupplier.setServletInfos(infos);
        CxfHandler handler = new CxfHandler(infos, beanContainer, httpConfiguration);

        final String cxfPath = normalizePath(infos.getPath());
        //LOGGER.infof("Mapping a Vert.x handler for CXF to %s as requested by %s", mappingPath, requestors);
        final Router router = routerValue.getValue();
        for (CXFServletInfo info : infos.getInfos()) {
            final String effectivePath = cxfPath + info.getRelativePath();
            LOGGER.tracef("Registering CXF route for path %s", effectivePath);
            final Route route = router
                    .route()
                    .path(effectivePath)
                    .blockingHandler(handler);
            shutdown.addShutdownTask(route::remove);
        }

    }

    static String normalizePath(String path) {
        if (path == null || path.isEmpty() || "/".equals(path)) {
            return "";
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return path;
    }

    public void resetDestinationRegistry(ShutdownContext context) {
        context.addShutdownTask(VertxDestinationFactory::resetRegistry);
    }
}
