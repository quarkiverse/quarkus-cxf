package io.quarkiverse.cxf.devui;

public class DevUiClientInfo {

    private final String configKey;
    private final String sei;
    private final String address;
    private final String wsdl;

    public DevUiClientInfo(String configKey, String sei, String address, String wsdl) {
        super();
        this.configKey = configKey;
        this.sei = sei;
        this.address = address;
        this.wsdl = wsdl;
    }

    public String getConfigKey() {
        return configKey;
    }

    public String getSei() {
        return sei;
    }

    public String getAddress() {
        return address;
    }

    public String getWsdl() {
        return wsdl;
    }

    @Override
    public String toString() {
        return "DevUiClientInfo [configKey=" + configKey + ", sei=" + sei + ", address=" + address + ", wsdl=" + wsdl + "]";
    }

}