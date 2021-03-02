package io.quarkiverse.cxf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkiverse.cxf.transport.CxfHandler;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class CXFRecorder {
    private static final Logger LOGGER = Logger.getLogger(CXFRecorder.class);

    public Supplier<CXFClientInfo> cxfClientInfoSupplier(String sei, CxfConfig cxfConfig,
            String soapBinding, String wsNamespace, String wsName, List<String> classNames) {
        LOGGER.trace("recorder CXFClientInfoSupplier");
        return () -> {
            // TODO if 2 clients with same SEI but different config it will failed but strange use case
            Map<String, CxfEndpointConfig> seiToCfg = new HashMap<>();
            Map<String, String> seiToPath = new HashMap<>();
            for (Map.Entry<String, CxfEndpointConfig> webServicesByPath : cxfConfig.endpoints.entrySet()) {
                CxfEndpointConfig cxfEndPointConfig = webServicesByPath.getValue();
                String relativePath = webServicesByPath.getKey();
                if (!cxfEndPointConfig.serviceInterface.isPresent()) {
                    continue;
                }
                String cfgSei = cxfEndPointConfig.serviceInterface.get();
                seiToCfg.put(cfgSei, cxfEndPointConfig);
                seiToPath.put(cfgSei, relativePath);
            }

            CxfEndpointConfig cxfEndPointConfig = seiToCfg.get(sei);
            String relativePath = seiToPath.get(sei);

            String endpointAddress;
            if (cxfEndPointConfig != null) {
                endpointAddress = cxfEndPointConfig.clientEndpointUrl.orElse("http://localhost:8080");
            } else {
                endpointAddress = "http://localhost:8080";
            }
            // default is sei name without package
            if (relativePath == null) {
                String serviceName = sei.toLowerCase();
                if (serviceName.contains(".")) {
                    serviceName = serviceName.substring(serviceName.lastIndexOf('.') + 1);
                }
                relativePath = "/" + serviceName;
            }
            if (!relativePath.equals("/") && !relativePath.equals("")) {
                endpointAddress = endpointAddress.endsWith("/")
                        ? endpointAddress.substring(0, endpointAddress.length() - 1)
                        : endpointAddress;
                endpointAddress = relativePath.startsWith("/") ? endpointAddress + relativePath
                        : endpointAddress + "/" + relativePath;
            }

            CXFClientInfo cfg = new CXFClientInfo();
            cfg.init(sei,
                    endpointAddress,
                    cxfEndPointConfig != null ? cxfEndPointConfig.wsdlPath.orElse(null) : null,
                    soapBinding,
                    wsNamespace,
                    wsName,
                    cxfEndPointConfig != null ? cxfEndPointConfig.endpointNamespace.orElse(null) : null,
                    cxfEndPointConfig != null ? cxfEndPointConfig.endpointName.orElse(null) : null,
                    cxfEndPointConfig != null ? cxfEndPointConfig.username.orElse(null) : null,
                    cxfEndPointConfig != null ? cxfEndPointConfig.password.orElse(null) : null,
                    classNames);
            if (cxfEndPointConfig != null && cxfEndPointConfig.inInterceptors.isPresent()) {
                cfg.getInInterceptors().addAll(cxfEndPointConfig.inInterceptors.get());
            }
            if (cxfEndPointConfig != null && cxfEndPointConfig.outInterceptors.isPresent()) {
                cfg.getOutInterceptors().addAll(cxfEndPointConfig.outInterceptors.get());
            }
            if (cxfEndPointConfig != null && cxfEndPointConfig.outFaultInterceptors.isPresent()) {
                cfg.getOutFaultInterceptors().addAll(cxfEndPointConfig.outFaultInterceptors.get());
            }
            if (cxfEndPointConfig != null && cxfEndPointConfig.inFaultInterceptors.isPresent()) {
                cfg.getInFaultInterceptors().addAll(cxfEndPointConfig.inFaultInterceptors.get());
            }
            if (cxfEndPointConfig != null && cxfEndPointConfig.features.isPresent()) {
                cfg.getFeatures().addAll(cxfEndPointConfig.features.get());
            }
            return cfg;
        };
    }

    public class servletConfig {
        public CxfEndpointConfig config;
        public String path;

        public servletConfig(CxfEndpointConfig cxfEndPointConfig, String relativePath) {
            this.config = cxfEndPointConfig;
            this.path = relativePath;
        }
    }

    public void registerCXFServlet(RuntimeValue<CXFServletInfos> runtimeInfos, String path, String sei,
            CxfConfig cxfConfig, String soapBinding, List<String> wrapperClassNames, String wsImplementor) {
        CXFServletInfos infos = runtimeInfos.getValue();
        Map<String, List<servletConfig>> implementorToCfg = new HashMap<>();
        for (Map.Entry<String, CxfEndpointConfig> webServicesByPath : cxfConfig.endpoints.entrySet()) {
            CxfEndpointConfig cxfEndPointConfig = webServicesByPath.getValue();
            String relativePath = webServicesByPath.getKey();
            if (!cxfEndPointConfig.implementor.isPresent()) {
                continue;
            }
            String cfgImplementor = cxfEndPointConfig.implementor.get();
            List<servletConfig> lst;
            if (implementorToCfg.containsKey(cfgImplementor)) {
                lst = implementorToCfg.get(cfgImplementor);
            } else {
                lst = new ArrayList<>();
                implementorToCfg.put(cfgImplementor, lst);
            }
            lst.add(new servletConfig(cxfEndPointConfig, relativePath));
        }
        List<servletConfig> cfgs = implementorToCfg.get(wsImplementor);
        if (cfgs != null) {
            for (servletConfig cfg : cfgs) {
                CxfEndpointConfig cxfEndPointConfig = cfg.config;
                String relativePath = cfg.path;
                startRoute(path, sei, soapBinding, wrapperClassNames, wsImplementor, infos, cxfEndPointConfig, relativePath);
            }
        } else {
            String serviceName = sei.toLowerCase();
            if (serviceName.contains(".")) {
                serviceName = serviceName.substring(serviceName.lastIndexOf('.') + 1);
            }
            String relativePath = "/" + serviceName;
            startRoute(path, sei, soapBinding, wrapperClassNames, wsImplementor, infos, null, relativePath);
        }
    }

    private void startRoute(String path, String sei, String soapBinding, List<String> wrapperClassNames, String wsImplementor,
            CXFServletInfos infos, CxfEndpointConfig cxfEndPointConfig, String relativePath) {
        if (wsImplementor != null && !wsImplementor.equals("")) {
            CXFServletInfo cfg = new CXFServletInfo(path,
                    relativePath,
                    wsImplementor,
                    sei,
                    cxfEndPointConfig != null ? cxfEndPointConfig.wsdlPath.orElse(null) : null,
                    soapBinding,
                    wrapperClassNames,
                    cxfEndPointConfig != null ? cxfEndPointConfig.publishedEndpointUrl.orElse(null) : null);
            if (cxfEndPointConfig != null && cxfEndPointConfig.inInterceptors.isPresent()) {
                cfg.getInInterceptors().addAll(cxfEndPointConfig.inInterceptors.get());
            }
            if (cxfEndPointConfig != null && cxfEndPointConfig.outInterceptors.isPresent()) {
                cfg.getOutInterceptors().addAll(cxfEndPointConfig.outInterceptors.get());
            }
            if (cxfEndPointConfig != null && cxfEndPointConfig.outFaultInterceptors.isPresent()) {
                cfg.getOutFaultInterceptors().addAll(cxfEndPointConfig.outFaultInterceptors.get());
            }
            if (cxfEndPointConfig != null && cxfEndPointConfig.inFaultInterceptors.isPresent()) {
                cfg.getInFaultInterceptors().addAll(cxfEndPointConfig.inFaultInterceptors.get());
            }
            if (cxfEndPointConfig != null && cxfEndPointConfig.features.isPresent()) {
                cfg.getFeatures().addAll(cxfEndPointConfig.features.get());
            }
            LOGGER.trace("register CXF Servlet info");
            infos.add(cfg);
        }
    }

    public RuntimeValue<CXFServletInfos> createInfos() {
        CXFServletInfos infos = new CXFServletInfos();
        return new RuntimeValue<>(infos);
    }

    public Handler<RoutingContext> initServer(RuntimeValue<CXFServletInfos> infos, BeanContainer beanContainer) {
        LOGGER.trace("init server");
        return new CxfHandler(infos.getValue(), beanContainer);
    }

    public void setPath(RuntimeValue<CXFServletInfos> infos, String path) {
        infos.getValue().setPath(path);
    }
}
