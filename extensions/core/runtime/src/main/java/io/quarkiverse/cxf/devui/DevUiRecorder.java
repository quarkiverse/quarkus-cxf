package io.quarkiverse.cxf.devui;

import java.util.List;

import io.quarkiverse.cxf.CXFServletInfos;
import io.quarkiverse.cxf.ClientInjectionPoint;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class DevUiRecorder {
    public RuntimeValue<List<ClientInjectionPoint>> clientInjectionPoints(List<ClientInjectionPoint> injectionPoints) {
        return new RuntimeValue<>(injectionPoints);
    }

    public void servletInfos(RuntimeValue<CXFServletInfos> cxfServletInfos) {
        CxfJsonRPCService.setServletInfos(cxfServletInfos.getValue());
    }

    public void shutdown(ShutdownContext context) {
        context.addShutdownTask(CxfJsonRPCService::shutdown);
    }
}
