package io.quarkiverse.cxf.deployment.devconsole;

import java.util.List;

import io.quarkiverse.cxf.deployment.CxfClientBuildItem;
import io.quarkiverse.cxf.deployment.CxfEndpointImplementationBuildItem;
import io.quarkiverse.cxf.devconsole.DevCxfClientInfosSupplier;
import io.quarkiverse.cxf.devconsole.DevCxfServerInfosSupplier;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.devconsole.spi.DevConsoleRuntimeTemplateInfoBuildItem;
import io.quarkus.devconsole.spi.DevConsoleTemplateInfoBuildItem;

public class DevConsoleProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    public void collectBeanInfo(
            List<CxfEndpointImplementationBuildItem> webservices,
            List<CxfClientBuildItem> clients,
            BuildProducer<DevConsoleTemplateInfoBuildItem> devConsoleTemplates) {
        devConsoleTemplates.produce(new DevConsoleTemplateInfoBuildItem("cxfClientBuildItems", clients));
        devConsoleTemplates.produce(new DevConsoleTemplateInfoBuildItem("cxfEndpointImplementationBuildItems", webservices));
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    public DevConsoleRuntimeTemplateInfoBuildItem collectClientBeanInfo(CurateOutcomeBuildItem curateOutcomeBuildItem) {
        return new DevConsoleRuntimeTemplateInfoBuildItem("cxfClientInfos", new DevCxfClientInfosSupplier(),
                DevConsoleProcessor.class, curateOutcomeBuildItem);
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    public DevConsoleRuntimeTemplateInfoBuildItem collectServerBeanInfo(CurateOutcomeBuildItem curateOutcomeBuildItem) {
        return new DevConsoleRuntimeTemplateInfoBuildItem("cxfServiceInfos", new DevCxfServerInfosSupplier(),
                DevConsoleProcessor.class, curateOutcomeBuildItem);
    }
}
