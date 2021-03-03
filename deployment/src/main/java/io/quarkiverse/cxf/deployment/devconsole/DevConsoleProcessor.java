package io.quarkiverse.cxf.deployment.devconsole;

import java.util.List;

import io.quarkiverse.cxf.deployment.CxfWebServiceBuildItem;
import io.quarkiverse.cxf.devconsole.DevCxfClientInfosSupplier;
import io.quarkiverse.cxf.devconsole.DevCxfServerInfosSupplier;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devconsole.spi.DevConsoleRuntimeTemplateInfoBuildItem;
import io.quarkus.devconsole.spi.DevConsoleTemplateInfoBuildItem;

public class DevConsoleProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    public DevConsoleTemplateInfoBuildItem collectBeanInfo(List<CxfWebServiceBuildItem> webservices) {
        return new DevConsoleTemplateInfoBuildItem("cxfBuildItemInfos", webservices);
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    public DevConsoleRuntimeTemplateInfoBuildItem collectClientBeanInfo() {
        return new DevConsoleRuntimeTemplateInfoBuildItem("cxfClientInfos", new DevCxfClientInfosSupplier());
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    public DevConsoleRuntimeTemplateInfoBuildItem collectServerBeanInfo() {
        return new DevConsoleRuntimeTemplateInfoBuildItem("cxfServiceInfos", new DevCxfServerInfosSupplier());
    }
}
