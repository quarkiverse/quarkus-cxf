package io.quarkiverse.cxf.deployment.devui;

import java.util.List;

import io.quarkiverse.cxf.deployment.CxfClientBuildItem;
import io.quarkiverse.cxf.deployment.CxfEndpointImplementationBuildItem;
import io.quarkiverse.cxf.devui.CxfJsonRPCService;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;

public class DevUIProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    public CardPageBuildItem pages(List<CxfEndpointImplementationBuildItem> services,
            List<CxfClientBuildItem> clients) {

        CardPageBuildItem cardPageBuildItem = new CardPageBuildItem();

        int total = services.size() + clients.size();

        cardPageBuildItem.addPage(Page.webComponentPageBuilder()
                .title("List of SOAP WS")
                .icon("font-awesome-solid:cubes")
                .componentLink("qwc-cxf-services.js")
                .staticLabel(String.valueOf(total)));

        return cardPageBuildItem;
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    JsonRPCProvidersBuildItem createJsonRPCServiceForCache() {
        return new JsonRPCProvidersBuildItem(CxfJsonRPCService.class);
    }
}
