package io.quarkiverse.cxf.it.server;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class XForwardedProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "quarkus.http.proxy.enable-forwarded-host", "true",
                "quarkus.http.proxy.enable-forwarded-prefix", "true",
                "quarkus.http.proxy.proxy-address-forwarding", "true");
    }

    @Override
    public List<TestResourceEntry> testResources() {
        return Collections.emptyList();
    }

    @Override
    public boolean disableGlobalTestResources() {
        return true;
    }

}
