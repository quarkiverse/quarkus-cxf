package io.quarkiverse.cxf.deployment.devconsole;

import java.util.List;

import io.quarkiverse.cxf.deployment.CxfWebServiceBuildItem;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devconsole.spi.DevConsoleTemplateInfoBuildItem;

public class DevConsoleProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    public DevConsoleTemplateInfoBuildItem collectBeanInfo(List<CxfWebServiceBuildItem> webservices) {
        DevCxfInfos cxfInfos = new DevCxfInfos();
        for (CxfWebServiceBuildItem webservice : webservices) {
            if (webservice.IsClient()) {
                cxfInfos.addClient(webservice.getSei());
            } else {
                cxfInfos.addService(webservice.getImplementor());
            }
        }
        return new DevConsoleTemplateInfoBuildItem("cxfInfo", cxfInfos);
    }
}
