package io.quarkiverse.cxf;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.servlet.ServletException;

import org.jboss.logging.Logger;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.core.ManagedServlet;

@Recorder
public class CXFRecorder {
    private static final Logger LOGGER = Logger.getLogger(CXFRecorder.class);

    public Supplier<CXFClientInfo> cxfClientInfoSupplier(String sei, CxfConfig cxfConfig,
            String soapBinding, String wsNamespace, String wsName, List<String> classNames) {
        LOGGER.warn("recorder CXFClientInfoSupplier");
        return new Supplier<CXFClientInfo>() {
            @Override
            public CXFClientInfo get() {
                // TODO suboptimal process. migrate to hashmap and get instead of loop
                for (Map.Entry<String, CxfEndpointConfig> webServicesByPath : cxfConfig.endpoints.entrySet()) {
                    CxfEndpointConfig cxfEndPointConfig = webServicesByPath.getValue();
                    String relativePath = webServicesByPath.getKey();
                    if (!cxfEndPointConfig.serviceInterface.isPresent()) {
                        continue;
                    }
                    String cfgSei = cxfEndPointConfig.serviceInterface.get();
                    if (cfgSei.equals(sei)) {
                        String endpointAddress = cxfEndPointConfig.clientEndpointUrl.orElse("http://localhost:8080");
                        if (!relativePath.equals("/") && !relativePath.equals("")) {
                            endpointAddress = endpointAddress.endsWith("/")
                                    ? endpointAddress.substring(0, endpointAddress.length() - 1)
                                    : endpointAddress;
                            endpointAddress = relativePath.startsWith("/") ? endpointAddress + relativePath
                                    : endpointAddress + "/" + relativePath;
                        }

                        CXFClientInfo cfg = new CXFClientInfo(sei,
                                endpointAddress,
                                cxfEndPointConfig.wsdlPath.orElse(null),
                                soapBinding,
                                wsNamespace,
                                wsName,
                                cxfEndPointConfig.endpointNamespace.orElse(null),
                                cxfEndPointConfig.endpointName.orElse(null),
                                cxfEndPointConfig.username.orElse(null),
                                cxfEndPointConfig.password.orElse(null),
                                classNames);
                        if (cxfEndPointConfig.inInterceptors.isPresent()) {
                            cfg.getInInterceptors().addAll(cxfEndPointConfig.inInterceptors.get());
                        }
                        if (cxfEndPointConfig.outInterceptors.isPresent()) {
                            cfg.getOutInterceptors().addAll(cxfEndPointConfig.outInterceptors.get());
                        }
                        if (cxfEndPointConfig.outFaultInterceptors.isPresent()) {
                            cfg.getOutFaultInterceptors().addAll(cxfEndPointConfig.outFaultInterceptors.get());
                        }
                        if (cxfEndPointConfig.inFaultInterceptors.isPresent()) {
                            cfg.getInFaultInterceptors().addAll(cxfEndPointConfig.inFaultInterceptors.get());
                        }
                        if (cxfEndPointConfig.features.isPresent()) {
                            cfg.getFeatures().addAll(cxfEndPointConfig.features.get());
                        }
                        return cfg;
                    }
                }
                LOGGER.warn("the service interface config is not found for : " + sei);
                return null;
            }
        };
    }

    public void registerCXFServlet(RuntimeValue<CXFServletInfos> runtimeInfos, String sei, CxfConfig cxfConfig,
            String soapBinding, List<String> wrapperClassNames, String wsImplementor) {
        CXFServletInfos infos = runtimeInfos.getValue();
        for (Map.Entry<String, CxfEndpointConfig> webServicesByPath : cxfConfig.endpoints.entrySet()) {
            CxfEndpointConfig cxfEndPointConfig = webServicesByPath.getValue();
            String relativePath = webServicesByPath.getKey();

            if (cxfEndPointConfig.implementor.isPresent()) {
                String implementor = cxfEndPointConfig.implementor.get();
                if (implementor != null && implementor.equals(wsImplementor)) {
                    CXFServletInfo cfg = new CXFServletInfo(relativePath,
                            implementor,
                            sei,
                            cxfEndPointConfig.wsdlPath.orElse(null),
                            soapBinding,
                            wrapperClassNames,
                            cxfEndPointConfig.publishedEndpointUrl.orElse(null));
                    if (cxfEndPointConfig.inInterceptors.isPresent()) {
                        cfg.getInInterceptors().addAll(cxfEndPointConfig.inInterceptors.get());
                    }
                    if (cxfEndPointConfig.outInterceptors.isPresent()) {
                        cfg.getOutInterceptors().addAll(cxfEndPointConfig.outInterceptors.get());
                    }
                    if (cxfEndPointConfig.outFaultInterceptors.isPresent()) {
                        cfg.getOutFaultInterceptors().addAll(cxfEndPointConfig.outFaultInterceptors.get());
                    }
                    if (cxfEndPointConfig.inFaultInterceptors.isPresent()) {
                        cfg.getInFaultInterceptors().addAll(cxfEndPointConfig.inFaultInterceptors.get());
                    }
                    if (cxfEndPointConfig.features.isPresent()) {
                        cfg.getFeatures().addAll(cxfEndPointConfig.features.get());
                    }

                    LOGGER.info("register CXF Servlet info");
                    infos.add(cfg);
                }
            }
        }
    }

    public RuntimeValue<CXFServletInfos> createInfos() {
        CXFServletInfos infos = new CXFServletInfos();
        return new RuntimeValue<>(infos);
    }

    public void initServlet(DeploymentManager deploymentMgr, RuntimeValue<CXFServletInfos> infos) {
        ManagedServlet managedServlet = deploymentMgr.getDeployment().getServlets()
                .getManagedServlet("org.apache.cxf.transport.servlet.CXFNonSpringServlet");
        try {
            if (managedServlet != null) {
                CXFQuarkusServlet servlet = (CXFQuarkusServlet) managedServlet.getServlet().getInstance();
                servlet.build(infos.getValue());
            }
        } catch (ServletException e) {
        }
    }
}
