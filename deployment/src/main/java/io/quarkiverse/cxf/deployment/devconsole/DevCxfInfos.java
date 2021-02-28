package io.quarkiverse.cxf.deployment.devconsole;

import java.util.ArrayList;
import java.util.List;

public class DevCxfInfos {
    private final List<String> services;
    private final List<String> clients;

    public DevCxfInfos() {
        services = new ArrayList<>();
        clients = new ArrayList<>();
    }

    public void addService(String implementor) {
        clients.add(implementor);
    }

    public void addClient(String sei) {
        clients.add(sei);
    }

    public List<String> getServices() {
        return services;
    }

    public List<String> getTClients() {
        return clients;
    }
}
