package io.quarkus.cxf;

import java.util.List;

import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class CXFServletRecorder {
    private static final Logger LOGGER = Logger.getLogger(CXFServletRecorder.class);

    public void registerCXFServlet(String path, String className,
            List<String> inInterceptors, List<String> outInterceptors, List<String> outFaultInterceptors,
            List<String> inFaultInterceptors, List<String> features, String sei, String wsdlPath, String soapBinding) {
        CXFServletInfo cfg = new CXFServletInfo(path, className, sei, wsdlPath, soapBinding);
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
