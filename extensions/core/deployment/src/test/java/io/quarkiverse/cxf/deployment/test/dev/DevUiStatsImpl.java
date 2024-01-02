package io.quarkiverse.cxf.deployment.test.dev;

import jakarta.inject.Inject;
import jakarta.jws.WebService;

import io.quarkiverse.cxf.devui.CxfJsonRPCService;

@WebService(name = "DevUiStats", serviceName = "DevUiStats")
public class DevUiStatsImpl implements DevUiStats {

    @Inject
    CxfJsonRPCService rpcService;

    @Override
    public int getClientCount() {
        return rpcService.getClientCount();
    }

    @Override
    public String getClient(int index) {
        return rpcService.getClients().get(index).toString();
    }

    @Override
    public int getServiceCount() {
        return rpcService.getServiceCount();
    }

    @Override
    public String getService(int index) {
        return rpcService.getServices().get(index).toString();
    }

}
