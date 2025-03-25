package io.quarkiverse.cxf.deployment.devui;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.jandex.DotName;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;

import io.quarkiverse.cxf.ClientInjectionPoint;
import io.quarkiverse.cxf.deployment.CXFServletInfosBuildItem;
import io.quarkiverse.cxf.deployment.CxfClientProcessor;
import io.quarkiverse.cxf.devui.CxfJsonRPCService;
import io.quarkiverse.cxf.devui.DevUiRecorder;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;

public class DevUIProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    public CardPageBuildItem pages() {

        CardPageBuildItem cardPageBuildItem = new CardPageBuildItem();

        cardPageBuildItem.addPage(Page.webComponentPageBuilder()
                .title("Clients")
                .icon("font-awesome-solid:message")
                .componentLink("qwc-cxf-clients.js")
                .dynamicLabelJsonRPCMethodName("getClientCount"));

        cardPageBuildItem.addPage(Page.webComponentPageBuilder()
                .title("Service Endpoints")
                .icon("font-awesome-solid:gears")
                .componentLink("qwc-cxf-services.js")
                .dynamicLabelJsonRPCMethodName("getServiceCount"));

        return cardPageBuildItem;
    }

    @BuildStep
    JsonRPCProvidersBuildItem createJsonRPCServiceForCache() {
        return new JsonRPCProvidersBuildItem(CxfJsonRPCService.class);
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    @Record(ExecutionTime.STATIC_INIT)
    void collectClients(
            CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<SyntheticBeanBuildItem> synthetics,
            DevUiRecorder recorder) {
        final List<ClientInjectionPoint> injectionPoints = CxfClientProcessor.findClientInjectionPoints(
                combinedIndexBuildItem.getIndex())
                .sorted(Comparator.comparing(ClientInjectionPoint::getConfigKey).thenComparing(cip -> cip.getSei().getName()))
                .collect(Collectors.toList());
        synthetics.produce(SyntheticBeanBuildItem
                .configure(List.class)
                .types(
                        ParameterizedType.create(
                                DotName.createSimple(List.class.getName()),
                                Type.create(DotName.createSimple(ClientInjectionPoint.class.getName()), Kind.CLASS)))
                .named("clientInjectionPoints")
                .runtimeValue(recorder.clientInjectionPoints(injectionPoints))
                .done());
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void collectServices(
            CXFServletInfosBuildItem infos,
            DevUiRecorder recorder) {
        recorder.servletInfos(infos.getCxfServletInfos());
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void shutDown(
            DevUiRecorder recorder,
            ShutdownContextBuildItem shutdownContext) {
        recorder.shutdown(shutdownContext);
    }

}
