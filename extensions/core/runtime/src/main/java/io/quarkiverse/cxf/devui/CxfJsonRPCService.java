package io.quarkiverse.cxf.devui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import io.quarkiverse.cxf.CXFServletInfos;
import io.quarkiverse.cxf.ClientInjectionPoint;
import io.quarkiverse.cxf.CxfClientConfig;
import io.quarkiverse.cxf.CxfClientProducer;
import io.quarkiverse.cxf.CxfClientProducer.ClientConfigSelection;
import io.quarkiverse.cxf.CxfConfig;
import io.quarkiverse.cxf.CxfFixedConfig;

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
            final String sei = ip.getSei().getName();
            /*
             * We intentionally do not create a CXFClientInfo here, because that would fail for clients that are not
             * configured properly, e.g. the ones without client-endpoint-url. We want to list those too.
             */
            final Optional<CxfClientConfig> clientConfig = CxfClientProducer
                    .selectClientConfig(config, fixedConfig, sei, ip.getConfigKey())
                    .map(ClientConfigSelection::config);

            /*
             * FIXME: (review) The address can be null now, which was not possible before this commit.
             * _addressRenderer() in extensions/core/deployment/src/main/resources/dev-ui/qwc-cxf-clients.js renders
             * <a href="${client.address}">${client.address}</a> unconditionally, so such a client gets an empty cell
             * with an empty link in Dev UI, without any hint that client-endpoint-url is missing.
             * Could the renderer show something like "client-endpoint-url not set" in that case?
             * DevUiTest covers only the JSON-RPC side.
             */
            final DevUiClientInfo devUiIInfo = new DevUiClientInfo(
                    ip.getConfigKey(),
                    sei,
                    clientConfig.flatMap(CxfClientConfig::clientEndpointUrl).orElse(null),
                    clientConfig.flatMap(CxfClientConfig::wsdlPath).orElse(null));

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
