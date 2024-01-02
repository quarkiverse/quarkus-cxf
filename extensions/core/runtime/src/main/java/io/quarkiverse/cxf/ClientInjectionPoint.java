package io.quarkiverse.cxf;

import java.util.Objects;

public class ClientInjectionPoint {
    private final String configKey;
    private final Class<?> sei;

    public ClientInjectionPoint(String configKey, Class<?> sei) {
        super();
        this.configKey = configKey;
        this.sei = sei;
    }

    public String getConfigKey() {
        return configKey;
    }

    public Class<?> getSei() {
        return sei;
    }

    @Override
    public int hashCode() {
        return Objects.hash(configKey, sei);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ClientInjectionPoint other = (ClientInjectionPoint) obj;
        return Objects.equals(configKey, other.configKey) && Objects.equals(sei, other.sei);
    }
}