package io.quarkiverse.cxf.deployment.test.dev;

import jakarta.enterprise.inject.Instance;
import jakarta.jws.WebService;

import io.quarkiverse.cxf.annotation.CXFClient;

@WebService(name = "DevUiStats", serviceName = "DevUiStats")
public class DevUiRemoteStatsImpl implements DevUiStats {

    @CXFClient("stats")
    DevUiStats stats;

    @CXFClient
    DevUiStats keyLessStats;

    /** A client without {@code client-endpoint-url} that cannot be created, but still has to be listed in Dev UI */
    @CXFClient("noUrl")
    Instance<DevUiStats> noUrlStats;

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
