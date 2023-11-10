package io.quarkiverse.cxf;

public enum EnabledFor {
    clients,
    services,
    clientsAndServices,
    none;

    public boolean enabledForAny() {
        return this != none;
    }

    public boolean enabledForServices() {
        return this == services || this == clientsAndServices;
    }

    public boolean enabledForClients() {
        return this == clients || this == clientsAndServices;
    }
}
