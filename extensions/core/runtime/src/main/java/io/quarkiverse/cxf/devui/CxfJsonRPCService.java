package io.quarkiverse.cxf.devui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import io.quarkiverse.cxf.CXFClientData;
import io.quarkiverse.cxf.CXFClientInfo;
import io.quarkiverse.cxf.CXFServletInfos;
import io.quarkiverse.cxf.ClientInjectionPoint;
import io.quarkiverse.cxf.CxfClientProducer;
import io.quarkiverse.cxf.CxfConfig;
import io.quarkiverse.cxf.CxfFixedConfig;
import io.quarkus.arc.Arc;

public class CxfJsonRPCService {

    private static List<DevUiServiceInfo> servletInfos = Collections.emptyList();

    @Inject
    @Named("clientInjectionPoints")
    List<ClientInjectionPoint> clientInjectionPoints;

    @Inject
    CxfConfig config;

    @Inject
    CxfFixedConfig fixedConfig;

    public List<DevUiServiceInfo> getServices() {
        return servletInfos;
    }

    public int getServiceCount() {
        return servletInfos.size();
    }

    public int getClientCount() {
        return clientInjectionPoints.size();
    }

    public List<DevUiClientInfo> getClients() {
        List<DevUiClientInfo> result = new ArrayList<>(clientInjectionPoints.size());
        for (ClientInjectionPoint ip : clientInjectionPoints) {
            final CXFClientData cxfClientData = (CXFClientData) Arc.container().instance(ip.getSei().getName()).get();

            final CXFClientInfo clientInfo = CxfClientProducer.selectorCXFClientInfo(
                    config,
                    fixedConfig,
                    cxfClientData,
                    ip.getConfigKey(),
                    () -> new IllegalStateException("Cannot find quarkus.cxf.client.\"" + ip.getConfigKey() + "\""));

            final DevUiClientInfo devUiIInfo = new DevUiClientInfo(
                    ip.getConfigKey(),
                    ip.getSei().getName(),
                    clientInfo.getEndpointAddress(),
                    clientInfo.getWsdlUrl());

            result.add(devUiIInfo);
        }
        result.sort(Comparator.comparing(DevUiClientInfo::getSei));
        return result;
    }

    public static void setServletInfos(CXFServletInfos infos) {
        servletInfos = Collections.unmodifiableList(
                infos.getInfos().stream()
                        .map(DevUiServiceInfo::of)
                        .sorted(Comparator.comparing(DevUiServiceInfo::getImplementor))
                        .collect(Collectors.toList()));
    }

    public static void shutdown() {
        servletInfos = Collections.emptyList();
    }

}
