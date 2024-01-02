package io.quarkiverse.cxf.deployment.test.dev;

import jakarta.jws.WebService;

import io.quarkiverse.cxf.annotation.CXFClient;

@WebService(name = "DevUiStats", serviceName = "DevUiStats")
public class DevUiRemoteStatsImpl implements DevUiStats {

    @CXFClient("stats")
    DevUiStats stats;

    @CXFClient
    DevUiStats keyLessStats;

    @Override
    public int getClientCount() {
        return stats.getClientCount();
    }

    @Override
    public String getClient(int index) {
        return stats.getClient(index);
    }

    @Override
    public int getServiceCount() {
        return stats.getServiceCount();
    }

    @Override
    public String getService(int index) {
        return stats.getService(index);
    }

}
