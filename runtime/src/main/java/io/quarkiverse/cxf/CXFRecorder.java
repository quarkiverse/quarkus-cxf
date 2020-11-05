package io.quarkiverse.cxf;

import java.util.List;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class CXFRecorder {
    private static final Logger LOGGER = Logger.getLogger(CXFRecorder.class);

    public Supplier<CXFClientInfo> CXFClientInfoSupplier(String sei, String endpointAddress, String wsdlUrl, String soapBinding,
            String wsNamespace, String wsName, String epNamespace, String epName, List<String> classNames) {
        LOGGER.warn("recorder CXFClientInfoSupplier");
        return new Supplier<CXFClientInfo>() {
            @Override
            public CXFClientInfo get() {
                return new CXFClientInfo(sei, endpointAddress, wsdlUrl, soapBinding, wsNamespace, wsName, epNamespace, epName,
                        classNames);
            }
        };
    }

    public void registerCXFServlet(String path, String className,
            List<String> inInterceptors, List<String> outInterceptors, List<String> outFaultInterceptors,
            List<String> inFaultInterceptors, List<String> features, String sei, String wsdlPath, String soapBinding,
            List<String> wrapperClassNames) {
        CXFServletInfo cfg = new CXFServletInfo(path, className, sei, wsdlPath, soapBinding, wrapperClassNames);
        cfg.getInInterceptors().addAll(inInterceptors);
        cfg.getOutInterceptors().addAll(outInterceptors);
        cfg.getOutFaultInterceptors().addAll(outFaultInterceptors);
        cfg.getInFaultInterceptors().addAll(inFaultInterceptors);
        cfg.getFeatures().addAll(features);
        LOGGER.info("register CXF Servlet info");
        CXFQuarkusServlet.publish(cfg);
        LOGGER.info("published CXF Servlet info");
    }
}
